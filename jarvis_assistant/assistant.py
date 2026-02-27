from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

from jarvis_assistant.device import DeviceController
from jarvis_assistant.models import ActionPlan, ExecutionResult
from jarvis_assistant.planner import IntentPlanner


@dataclass
class AssistantConfig:
    wake_word: str = "jarvis"


class JarvisAssistant:
    def __init__(self, device: DeviceController, planner: IntentPlanner, config: AssistantConfig | None = None):
        self.device = device
        self.planner = planner
        self.config = config or AssistantConfig()

    def is_wake_phrase(self, speech_text: str) -> bool:
        return self.config.wake_word.lower() in speech_text.lower().strip()

    def handle_command(self, speech_text: str, confirm: Callable[[ActionPlan], bool]) -> ExecutionResult:
        plan = self.planner.build_plan(speech_text)
        if plan.requires_confirmation and not confirm(plan):
            return ExecutionResult(ok=False, summary="Cancelled by user confirmation gate.")
        return self.device.execute(plan)
