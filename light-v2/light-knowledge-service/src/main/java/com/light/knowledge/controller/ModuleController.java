package com.light.knowledge.controller;

import com.light.common.result.Result;
import com.light.knowledge.entity.Module;
import com.light.knowledge.service.ModuleService;
import com.light.knowledge.dto.ModuleCreateRequest;
import com.light.knowledge.dto.ModuleUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/module")
@RequiredArgsConstructor
public class ModuleController {
    private final ModuleService moduleService;

    @GetMapping("/list")
    public Result<List<Module>> list() { return Result.success(moduleService.list()); }

    @GetMapping("/{id}")
    public Result<Module> detail(@PathVariable Long id) { return Result.success(moduleService.getById(id)); }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody ModuleCreateRequest request) {
        Module module = new Module();
        org.springframework.beans.BeanUtils.copyProperties(request, module);
        moduleService.save(module); return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ModuleUpdateRequest request) {
        Module module = new Module();
        org.springframework.beans.BeanUtils.copyProperties(request, module);
        moduleService.update(id, module); return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { moduleService.delete(id); return Result.success(); }
}
