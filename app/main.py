from __future__ import annotations

from flask import Flask, jsonify, render_template, request

from app.security import (
    IdentityContext,
    IdentityGuard,
    RateLimiterAnomalyDetector,
    SafeModeController,
    SecureLogger,
)

ALLOWED_PERMISSIONS = {"contacts", "microphone", "location", "camera", "notifications"}


def create_app() -> Flask:
    app = Flask(__name__)

    app.config["identity_guard"] = IdentityGuard(min_confidence=0.8)
    app.config["safe_mode"] = SafeModeController(enabled=False)
    app.config["rate_detector"] = RateLimiterAnomalyDetector(max_attempts=3, window_seconds=60, lockout_seconds=180)
    app.config["logger"] = SecureLogger()
    app.config["permissions"] = {perm: True for perm in ALLOWED_PERMISSIONS}

    @app.get("/")
    def dashboard() -> str:
        return render_template("dashboard.html")

    @app.get("/api/privacy/data-summary")
    def data_summary():
        logger: SecureLogger = app.config["logger"]
        safe_mode: SafeModeController = app.config["safe_mode"]
        permissions = app.config["permissions"]
        entries = logger.read_entries()
        return jsonify(
            {
                "stored_data": {
                    "encrypted_logs": len(entries),
                    "permissions": permissions,
                    "safe_mode": safe_mode.enabled,
                },
                "controls": {
                    "clear_history": True,
                    "quick_revoke_permissions": sorted(ALLOWED_PERMISSIONS),
                },
            }
        )

    @app.post("/api/privacy/clear-history")
    def clear_history():
        logger: SecureLogger = app.config["logger"]
        logger.clear()
        return jsonify({"ok": True, "message": "history cleared"})

    @app.post("/api/privacy/revoke-permissions")
    def revoke_permissions():
        logger: SecureLogger = app.config["logger"]
        permissions = app.config["permissions"]
        data = request.get_json(silent=True) or {}
        requested = [p for p in data.get("permissions", []) if p in ALLOWED_PERMISSIONS]

        for perm in requested:
            permissions[perm] = False

        logger.write("permissions_revoked", {"permissions": requested})
        return jsonify({"ok": True, "permissions": permissions, "revoked": requested})

    @app.post("/api/safe-mode")
    def set_safe_mode():
        logger: SecureLogger = app.config["logger"]
        safe_mode: SafeModeController = app.config["safe_mode"]

        data = request.get_json(silent=True) or {}
        enabled = bool(data.get("enabled", False))
        safe_mode.set_mode(enabled)
        logger.write("safe_mode_toggled", {"enabled": enabled})
        return jsonify({"ok": True, "enabled": enabled})

    @app.post("/api/action")
    def run_action():
        logger: SecureLogger = app.config["logger"]
        identity_guard: IdentityGuard = app.config["identity_guard"]
        safe_mode: SafeModeController = app.config["safe_mode"]
        rate_detector: RateLimiterAnomalyDetector = app.config["rate_detector"]

        data = request.get_json(silent=True) or {}
        actor_id = str(data.get("actor_id", "unknown"))
        action = str(data.get("action", "informational"))
        destructive = bool(data.get("destructive", False))
        sensitive = bool(data.get("sensitive", False))
        target_count = max(1, int(data.get("target_count", 1)))

        identity = IdentityContext(
            confidence=float(data.get("identity_confidence", 1.0)),
            pin_verified=bool(data.get("pin_verified", False)),
            biometric_verified=bool(data.get("biometric_verified", False)),
        )

        allowed, reason = identity_guard.require_step_up(identity, sensitive)
        if not allowed:
            logger.write("action_blocked_identity", data)
            return jsonify({"ok": False, "reason": reason}), 403

        allowed, reason = safe_mode.action_allowed(action, destructive)
        if not allowed:
            logger.write("action_blocked_safe_mode", data)
            return jsonify({"ok": False, "reason": reason}), 403

        allowed, reason = rate_detector.evaluate(actor_id, action, target_count)
        if not allowed:
            logger.write("action_blocked_anomaly", data)
            return jsonify({"ok": False, "reason": reason}), 429

        logger.write("action_executed", data)
        return jsonify({"ok": True, "reason": "action executed"})

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=False)
