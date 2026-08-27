package com.zooearly.album;

import com.zooearly.common.exception.BusinessException;
import com.zooearly.common.response.ApiResponse;
import com.zooearly.common.response.ErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 동화 앨범 — 아이가 만든 동화를 남기고 다시 본다.
 *
 * <h2>왜 /api/v1/ai 아래가 아닌가</h2>
 * {@code /api/v1/ai/*} 는 "추론 서버로 중계한다"는 뜻이다. 앨범은 추론이 아니라
 * 게이트웨이가 스스로 가진 데이터라, 이름이 뜻과 맞아야 나중에 읽는 사람이 헷갈리지
 * 않는다. 릴레이와 달리 여기서는 body 를 열어 보고 응답도 게이트웨이가 만든다.
 *
 * <h2>childId 가 곧 열쇠다</h2>
 * 로그인이 없다. 기기가 처음 켜질 때 만든 UUID 로 앨범을 묶으므로, 조회할 때도
 * 반드시 그 값을 함께 요구한다 — id 만으로 꺼내게 두면 번호를 하나씩 올려보는
 * 것만으로 남의 동화를 읽을 수 있다.
 */
@RestController
@RequestMapping("/api/v1/albums")
public class AlbumController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final AlbumService service;

    public AlbumController(AlbumService service) {
        this.service = service;
    }

    /** 동화를 앨범에 남긴다. 앱이 동화를 화면에 띄운 뒤에 부른다. */
    @PostMapping
    public ResponseEntity<ApiResponse<AlbumDtos.SaveResult>> save(@Valid @RequestBody AlbumDtos.SaveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.save(request)));
    }

    /** 이 아이의 앨범 목록. 최신순. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlbumDtos.Summary>>> list(@RequestParam String childId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(requireUuid(childId))));
    }

    /** 한 편 전체. 앱이 이걸로 동화 화면을 그대로 다시 그린다. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AlbumDtos.Detail>> detail(
            @PathVariable long id, @RequestParam String childId) {
        return ResponseEntity.ok(ApiResponse.ok(service.detail(id, requireUuid(childId))));
    }

    /**
     * 쿼리 파라미터에는 @Valid 가 걸리지 않아 여기서 직접 본다.
     *
     * 모양을 안 보면 childId 자리에 아무 문자열이나 넣어 조회를 시도할 수 있고,
     * 그러면 인덱스를 훑는 헛질의만 늘어난다.
     */
    private String requireUuid(String childId) {
        if (childId == null || !childId.matches(UUID_PATTERN)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "childId");
        }
        return childId;
    }
}
