from __future__ import annotations

import argparse

from jarvis_assistant.assistant import JarvisAssistant
from jarvis_assistant.device import DeviceController
from jarvis_assistant.models import ActionPlan
from jarvis_assistant.planner import IntentPlanner


def ask_confirmation(plan: ActionPlan) -> bool:
    print(f"Confirmation required for action: {plan.action.value} with params={plan.params}")
    reply = input("Proceed? (yes/no): ").strip().lower()
    return reply in {"yes", "y"}


def main() -> None:
    parser = argparse.ArgumentParser(description="Jarvis phone-control assistant")
    parser.add_argument("command", help="Spoken command text (must include wake word for realistic flows)")
    args = parser.parse_args()

    assistant = JarvisAssistant(device=DeviceController(), planner=IntentPlanner())

    if not assistant.is_wake_phrase(args.command):
        print("Wake word not detected. Say 'Jarvis ...'")
        return

    cleaned = args.command.lower().replace("jarvis", "", 1).strip()
    result = assistant.handle_command(cleaned, confirm=ask_confirmation)
    print(result.summary)


if __name__ == "__main__":
    main()
