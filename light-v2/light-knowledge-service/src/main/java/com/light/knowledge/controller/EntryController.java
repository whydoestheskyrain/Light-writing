package com.light.knowledge.controller;

import com.light.common.result.Result;
import com.light.common.result.PageResult;
import com.light.knowledge.dto.EntryCreateRequest;
import com.light.knowledge.dto.EntryUpdateRequest;
import com.light.knowledge.entity.Entry;
import com.light.knowledge.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/entry")
@RequiredArgsConstructor
public class EntryController {
    private final EntryService entryService;

    @GetMapping("/list")
    public Result<PageResult<Entry>> list(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "20") long size,
                                    @RequestParam(required = false) Long moduleId,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String tag) {
        if (current < 1 || size < 1 || size > 100) {
            throw new com.light.common.exception.BusinessException(com.light.common.result.ErrorCode.BAD_REQUEST,
                    "current必须大于0，size必须在1到100之间");
        }
        return Result.success(entryService.page(current, size, moduleId, status, keyword, tag));
    }

    @GetMapping("/tags")
    public Result<java.util.List<String>> tags() { return Result.success(entryService.tags()); }

    @GetMapping("/{id}")
    public Result<Entry> detail(@PathVariable Long id) { return Result.success(entryService.getById(id)); }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody EntryCreateRequest request) {
        Entry entry = new Entry();
        org.springframework.beans.BeanUtils.copyProperties(request, entry);
        entryService.save(entry); return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody EntryUpdateRequest request) {
        Entry entry = new Entry();
        org.springframework.beans.BeanUtils.copyProperties(request, entry);
        entryService.update(id, entry); return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { entryService.delete(id); return Result.success(); }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        entryService.updateStatus(id, body.get("status"));
        return Result.success();
    }
}
