package com.ablmvp.sprintassistant.live;

import com.ablmvp.sprintassistant.live.model.AskRequest;
import com.ablmvp.sprintassistant.live.model.AskResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final AiAssistantService assistant;
    public AssistantController(AiAssistantService assistant) { this.assistant = assistant; }
    @PostMapping("/ask") public AskResponse ask(@Valid @RequestBody AskRequest request) { return assistant.ask(request.question()); }
}
