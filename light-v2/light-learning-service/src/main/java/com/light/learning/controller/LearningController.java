package com.light.learning.controller;
import com.light.common.result.Result;
import com.light.learning.dto.HeartbeatRequest;
import com.light.learning.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/learning") @RequiredArgsConstructor
public class LearningController { private final LearningService service; @PostMapping("/heartbeat") public Result<Void> heartbeat(@RequestBody HeartbeatRequest req){service.heartbeat(req);return Result.success();} @GetMapping("/summary") public Result<?> summary(@RequestParam(defaultValue="today") String range){return Result.success(service.summary(range));} @GetMapping("/records") public Result<?> records(){return Result.success(service.records());} }
