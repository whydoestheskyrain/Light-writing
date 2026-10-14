package com.virtuallearn.knowledge.controller;

import com.virtuallearn.knowledge.common.Result;
import com.virtuallearn.knowledge.entity.Entry;
import com.virtuallearn.knowledge.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entry")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @GetMapping("/list")
    public Result<List<Entry>> list(@RequestParam(required = false) Long moduleId,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String keyword) {
        return Result.success(entryService.list(moduleId, status, keyword));
    }

    @GetMapping("/{id}")
    public Result<Entry> detail(@PathVariable Long id) {
        return Result.success(entryService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Entry entry) {
        entryService.save(entry);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Entry entry) {
        entry.setId(id);
        entryService.update(entry);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        entryService.delete(id);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        entryService.updateStatus(id, status);
        return Result.success();
    }
}
