package com.moemoe.core.service;

import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.TagEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    /**
     * 주어진 태그 이름 목록에 대해 연관된 상품 수를 증가시킵니다.
     * <p>
     * 각 태그 이름에 대해 다음을 수행합니다:
     * <ul>
     *   <li>이미 존재하는 {@link TagEntity}가 있는 경우, 해당 엔티티의 {@code productsCount}를 1씩 증가시킵니다.</li>
     *   <li>존재하지 않는 경우, 초기 {@code productsCount}가 1인 새 {@link TagEntity}를 생성하여 저장합니다.</li>
     * </ul>
     *
     * @param tagNames 처리할 태그 이름 목록. {@code null}이거나 요소로 {@code null}을 포함해서는 안 됩니다.
     * @throws NullPointerException {@code tagNames} 또는 그 목록의 요소가 {@code null}인 경우 발생할 수 있습니다.
     */
    public void incrementProductsCount(List<String> tagNames) {
        for (String tagName : tagNames) {
            Optional<TagEntity> optionalTag = tagEntityRepository.findTagEntityByName(tagName);
            if (optionalTag.isPresent()) {
                tagEntityRepository.incrementProductsCount(tagName);
            } else {
                tagEntityRepository.save(TagEntity.of(tagName, 1L));
            }
        }
    }
}
