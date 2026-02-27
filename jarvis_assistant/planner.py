from __future__ import annotations

import re

from jarvis_assistant.models import ActionPlan, ActionType, RISKY_ACTIONS


class IntentPlanner:
    """Simple natural-language planner.

    This is intentionally deterministic so you can replace it with an LLM later,
    while keeping the same ActionPlan contract.
    """

    def build_plan(self, user_text: str) -> ActionPlan:
        text = user_text.strip()
        lowered = text.lower()

        if "whatsapp" in lowered and "group" in lowered and any(
            token in lowered for token in ["check", "read", "see", "find"]
        ):
            group = self._extract_group_name(text)
            needle = self._extract_after_keywords(lowered, ["message", "like", "about"])
            plan = ActionPlan(
                action=ActionType.CHECK_WHATSAPP_GROUP,
                params={"group": group, "contains": needle},
            )
            plan.requires_confirmation = plan.action in RISKY_ACTIONS
            return plan

        if "send" in lowered and "message" in lowered:
            target = self._extract_after_keywords(text, ["to"]) or "unknown"
            body = self._extract_after_keywords(text, ["saying", "that", "message"])
            plan = ActionPlan(
                action=ActionType.SEND_MESSAGE,
                params={"target": target, "body": body or ""},
            )
            plan.requires_confirmation = True
            return plan

        if lowered.startswith("search") or "find on the web" in lowered:
            query = re.sub(r"^search", "", text, flags=re.IGNORECASE).strip()
            return ActionPlan(
                action=ActionType.WEB_SEARCH,
                params={"query": query or text},
                requires_confirmation=False,
            )

        if lowered.startswith("open "):
            app_name = text[5:].strip()
            return ActionPlan(action=ActionType.OPEN_APP, params={"app": app_name})

        if "delete" in lowered:
            return ActionPlan(
                action=ActionType.DELETE_ITEM,
                params={"target": text},
                requires_confirmation=True,
            )

        if "upload" in lowered:
            return ActionPlan(
                action=ActionType.UPLOAD_ITEM,
                params={"target": text},
                requires_confirmation=True,
            )

        return ActionPlan(action=ActionType.UNKNOWN, params={"raw": text})

    @staticmethod
    def _extract_after_keywords(text: str, keywords: list[str]) -> str | None:
        for keyword in keywords:
            idx = text.lower().find(keyword.lower())
            if idx != -1:
                return text[idx + len(keyword) :].strip(" :,-")
        return None

    @staticmethod
    def _extract_group_name(text: str) -> str:
        quote_match = re.search(r"['\"]([^'\"]+)['\"]", text)
        if quote_match:
            return quote_match.group(1)

        group_match = re.search(r"group\s+([\w\s]+)", text, flags=re.IGNORECASE)
        if group_match:
            return group_match.group(1).strip()

        return "unknown"
