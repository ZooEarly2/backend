package com.zooearly.album;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 앨범 조회는 언제나 <b>아이 단위</b>다.
 *
 * findById 만 쓰는 메서드를 두지 않았다. 로그인이 없어 childId 가 곧 열쇠이므로,
 * id 만으로 꺼낼 수 있게 두면 번호를 하나씩 올려보는 것만으로 남의 동화를 읽을 수 있다.
 * 한 편을 볼 때도 childId 를 함께 요구한다.
 */
public interface StoryAlbumRepository extends JpaRepository<StoryAlbum, Long> {

    List<StoryAlbum> findByChildIdOrderByCreatedAtDesc(String childId);

    Optional<StoryAlbum> findByIdAndChildId(Long id, String childId);

    long countByChildId(String childId);
}
