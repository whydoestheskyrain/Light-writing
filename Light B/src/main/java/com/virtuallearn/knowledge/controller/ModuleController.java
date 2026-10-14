package com.virtuallearn.knowledge.controller;

import com.virtuallearn.knowledge.common.Result;
import com.virtuallearn.knowledge.entity.Module;
import com.virtuallearn.knowledge.service.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/module")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping("/list")
    public Result<List<Module>> list() {
        return Result.success(moduleService.list());
    }

    @GetMapping("/{id}")
    public Result<Module> detail(@PathVariable Long id) {
        return Result.success(moduleService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Module module) {
        moduleService.save(module);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Module module) {
        module.setId(id);
        moduleService.update(module);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        moduleService.delete(id);
        return Result.success();
    }
}
