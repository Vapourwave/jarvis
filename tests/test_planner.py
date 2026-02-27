from jarvis_assistant.models import ActionType
from jarvis_assistant.planner import IntentPlanner


def test_send_message_requires_confirmation() -> None:
    planner = IntentPlanner()
    plan = planner.build_plan("send message to John saying hi")
    assert plan.action == ActionType.SEND_MESSAGE
    assert plan.requires_confirmation is True


def test_whatsapp_group_check_parsing() -> None:
    planner = IntentPlanner()
    plan = planner.build_plan("check my whatsapp group 'Family' and see if message like invoice is there")
    assert plan.action == ActionType.CHECK_WHATSAPP_GROUP
    assert plan.params["group"] == "Family"
    assert "invoice" in (plan.params["contains"] or "")
