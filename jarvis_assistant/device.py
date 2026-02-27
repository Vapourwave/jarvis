from __future__ import annotations

import subprocess
import tempfile
import xml.etree.ElementTree as ET
from dataclasses import dataclass

from jarvis_assistant.models import ActionPlan, ActionType, ExecutionResult


@dataclass
class DeviceController:
    """Phone operations via ADB.

    Methods are intentionally explicit and auditable: each risky action can be
    intercepted by the Assistant for final confirmation.
    """

    adb_bin: str = "adb"

    def execute(self, plan: ActionPlan) -> ExecutionResult:
        if plan.action == ActionType.OPEN_APP:
            return self._open_app(plan.params.get("app", ""))
        if plan.action == ActionType.SEND_MESSAGE:
            return self._send_message(plan.params.get("target", ""), plan.params.get("body", ""))
        if plan.action == ActionType.CHECK_WHATSAPP_GROUP:
            return self._check_whatsapp_group(
                group_name=plan.params.get("group", "unknown"),
                contains=plan.params.get("contains"),
            )
        if plan.action == ActionType.WEB_SEARCH:
            return self._web_search(plan.params.get("query", ""))
        if plan.action in {ActionType.DELETE_ITEM, ActionType.UPLOAD_ITEM}:
            return ExecutionResult(
                ok=False,
                summary="Operation is app-specific. Connect a concrete connector first.",
                payload={"action": plan.action.value},
            )
        return ExecutionResult(ok=False, summary="Unsupported command.")

    def _run(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run([self.adb_bin, *args], text=True, capture_output=True, check=False)

    def _open_app(self, app: str) -> ExecutionResult:
        package = self._app_to_package(app)
        if not package:
            return ExecutionResult(ok=False, summary=f"Unknown app: {app}")
        proc = self._run("shell", "monkey", "-p", package, "-c", "android.intent.category.LAUNCHER", "1")
        ok = proc.returncode == 0
        return ExecutionResult(ok=ok, summary=("Opened" if ok else "Failed opening") + f" {app}")

    def _send_message(self, target: str, body: str) -> ExecutionResult:
        uri = f"sms:{target}"
        proc = self._run(
            "shell",
            "am",
            "start",
            "-a",
            "android.intent.action.SENDTO",
            "-d",
            uri,
            "--es",
            "sms_body",
            body,
        )
        ok = proc.returncode == 0
        return ExecutionResult(
            ok=ok,
            summary=f"Prepared message to {target}. Please verify on-screen send button.",
            payload={"target": target, "body": body},
        )

    def _web_search(self, query: str) -> ExecutionResult:
        proc = self._run(
            "shell",
            "am",
            "start",
            "-a",
            "android.intent.action.VIEW",
            "-d",
            f"https://www.google.com/search?q={query.replace(' ', '+')}",
        )
        return ExecutionResult(ok=proc.returncode == 0, summary=f"Opened web search for: {query}")

    def _check_whatsapp_group(self, group_name: str, contains: str | None) -> ExecutionResult:
        self._open_app("whatsapp")
        dump_proc = self._run("exec-out", "uiautomator", "dump", "/dev/tty")
        if dump_proc.returncode != 0 or "<hierarchy" not in dump_proc.stdout:
            return ExecutionResult(ok=False, summary="Could not read WhatsApp UI. Ensure ADB + accessibility are enabled.")

        with tempfile.NamedTemporaryFile(mode="w+", suffix=".xml", delete=True) as tmp:
            tmp.write(dump_proc.stdout)
            tmp.flush()
            root = ET.parse(tmp.name).getroot()

        texts = [node.attrib.get("text", "") for node in root.iter("node") if node.attrib.get("text")]
        normalized = "\n".join(texts).lower()
        exists_group = group_name.lower() in normalized if group_name and group_name != "unknown" else True
        contains_match = (contains.lower() in normalized) if contains else None

        summary = f"Scanned WhatsApp UI for group '{group_name}'."
        if contains:
            summary += f" Match for '{contains}': {'yes' if contains_match else 'no'}."
        if not exists_group:
            summary += " Group not found in current visible UI."

        return ExecutionResult(
            ok=exists_group and (contains_match if contains_match is not None else True),
            summary=summary,
            payload={"visible_text_count": len(texts), "contains_match": contains_match},
        )

    @staticmethod
    def _app_to_package(app: str) -> str | None:
        mapping = {
            "whatsapp": "com.whatsapp",
            "chrome": "com.android.chrome",
            "messages": "com.google.android.apps.messaging",
            "telegram": "org.telegram.messenger",
            "gmail": "com.google.android.gm",
        }
        return mapping.get(app.strip().lower())
