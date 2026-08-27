package com.zooearly.album;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zooearly.common.exception.BusinessException;
import com.zooearly.common.response.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동화 앨범.
 *
 * <h2>왜 동화 생성 응답을 가로채 저장하지 않고, 앱이 다시 올리나</h2>
 * 릴레이(/api/v1/ai/*)는 응답 body 를 열지 않는다는 것이 이 게이트웨이의 뼈대다.
 * 저장하려고 그 자리에서 body 를 파싱하면 그 뼈대가 무너지고, 추론 서버의 응답
 * 스키마가 바뀔 때마다 게이트웨이가 같이 깨진다.
 *
 * 그래서 앨범은 <b>별도의 자기 API</b>로 둔다. 앱은 동화를 받아 화면에 띄운 뒤
 * 그것을 앨범에 올린다. 덤으로 생기는 이득이 있다 — 저장이 실패해도 아이는 이미
 * 동화를 보고 있다. 읽는 일과 남기는 일이 서로를 막지 않는다.
 *
 * <h2>왜 한 아이가 남길 수 있는 편수를 막나</h2>
 * 인증이 없는 API 라 childId 만 바꿔가며 무한히 밀어 넣을 수 있다. 한 아이가
 * 하루에 한 편을 만드는 앱이므로 넉넉히 잡아도 상한이 있는 편이 안전하다.
 */
@Service
public class AlbumService {

    /** 한 아이가 남길 수 있는 최대 편수. 하루 한 편이면 1년 반 치다. */
    private static final int MAX_PER_CHILD = 500;

    private final StoryAlbumRepository repository;
    private final ObjectMapper objectMapper;

    public AlbumService(StoryAlbumRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AlbumDtos.SaveResult save(AlbumDtos.SaveRequest request) {
        if (repository.countByChildId(request.childId()) >= MAX_PER_CHILD) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "childId");
        }
        String scenesJson = write(request.scenes());
        StoryAlbum saved = repository.save(new StoryAlbum(
                request.childId(),
                request.nickname().strip(),
                request.title().strip(),
                scenesJson,
                Instant.now()));
        return new AlbumDtos.SaveResult(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<AlbumDtos.Summary> list(String childId) {
        return repository.findByChildIdOrderByCreatedAtDesc(childId).stream()
                .map(album -> new AlbumDtos.Summary(
                        album.getId(),
                        album.getTitle(),
                        album.getNickname(),
                        album.getCreatedAt().toString(),
                        // 목록에서는 보석 줄만 그리면 되므로 장면 종류만 꺼낸다
                        read(album.getScenesJson()).stream()
                                .map(AlbumDtos.Scene::category)
                                .toList()))
                .toList();
    }

    /**
     * 한 편 전체.
     *
     * id 만으로 찾지 않고 childId 를 함께 요구한다 — 인증이 없어 번호를 하나씩
     * 올려보는 것만으로 남의 동화를 읽을 수 있기 때문이다.
     */
    @Transactional(readOnly = true)
    public AlbumDtos.Detail detail(long id, String childId) {
        StoryAlbum album = repository
                .findByIdAndChildId(id, childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "id"));
        return new AlbumDtos.Detail(
                album.getId(),
                album.getTitle(),
                album.getNickname(),
                album.getCreatedAt().toString(),
                read(album.getScenesJson()));
    }

    private String write(List<AlbumDtos.Scene> scenes) {
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "scenes");
        }
    }

    private List<AlbumDtos.Scene> read(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AlbumDtos.Scene>>() {});
        } catch (JsonProcessingException e) {
            // 저장할 때 우리가 직렬화한 값이라 여기서 깨지면 데이터가 상한 것이다.
            throw new IllegalStateException("앨범 데이터를 읽을 수 없습니다: " + e.getOriginalMessage(), e);
        }
    }
}
