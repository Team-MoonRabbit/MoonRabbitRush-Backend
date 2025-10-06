package com.moon.rabbit.domain.user.controller;

import com.moon.rabbit.domain.user.dto.RankResponse;
import com.moon.rabbit.domain.user.dto.ScoreRequest;
import com.moon.rabbit.domain.user.entity.User;
import com.moon.rabbit.domain.user.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    @PostMapping("/score")
    public ResponseEntity<User> updateScore(
            @RequestBody ScoreRequest request
    ) {
        User updatedUser = gameService.updateScore(request.score());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<RankResponse>> getRanking() {
        List<RankResponse> ranking = gameService.getRanking();
        return ResponseEntity.ok(ranking);
    }
}
