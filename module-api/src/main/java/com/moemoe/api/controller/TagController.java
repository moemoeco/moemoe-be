package com.moemoe.api.controller;

import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.core.service.TagService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping
    public GetTagsResponse getTags(
            @NotEmpty @RequestParam(name = "prefix") String prefix
    ) {
        return tagService.findTop20TagsByPrefix(prefix);
    }
}
