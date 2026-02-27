from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any


class ActionType(str, Enum):
    OPEN_APP = "open_app"
    SEND_MESSAGE = "send_message"
    CHECK_WHATSAPP_GROUP = "check_whatsapp_group"
    WEB_SEARCH = "web_search"
    DELETE_ITEM = "delete_item"
    UPLOAD_ITEM = "upload_item"
    UNKNOWN = "unknown"


RISKY_ACTIONS = {
    ActionType.SEND_MESSAGE,
    ActionType.DELETE_ITEM,
    ActionType.UPLOAD_ITEM,
}


@dataclass
class ActionPlan:
    action: ActionType
    params: dict[str, Any] = field(default_factory=dict)
    requires_confirmation: bool = False


@dataclass
class ExecutionResult:
    ok: bool
    summary: str
    payload: dict[str, Any] = field(default_factory=dict)
