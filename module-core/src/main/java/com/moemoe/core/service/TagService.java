package com.moemoe.core.service;

import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.mongo.repository.TagEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagService {
    private final TagEntityRepository tagEntityRepository;

    public GetTagsResponse findTop20TagsByPrefix(String prefix) {
        Sort defaultSort = Sort.by(Sort.Order.desc("productsCount"), Sort.Order.asc("name"));
        List<GetTagsResponse.Tag> content = tagEntityRepository.findTop20ByNameStartingWith(prefix, defaultSort)
                .stream()
                .map(tag -> GetTagsResponse.Tag.of(tag.getName(), tag.getProductsCount()))
                .toList();
        return GetTagsResponse.of(content);
    }
}
