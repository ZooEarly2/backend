package com.zooearly.album;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 아이가 만든 동화 한 편.
 *
 * <h2>왜 닉네임이 아니라 childId 로 묶나</h2>
 * 닉네임은 <b>겹치고 바뀐다.</b> 같은 반에 "지우"가 둘이면 앨범이 섞이고, 메뉴에서
 * 이름을 바꾸는 순간 과거 앨범을 잃는다. 게다가 아이 이름이 모든 조회 키에 실려 다닌다.
 * 그래서 기기가 처음 켜질 때 만든 UUID 로 묶고, 닉네임은 <b>그때 그 이름</b>을 보여주기
 * 위한 표시용 값으로만 함께 남긴다 — 나중에 이름을 바꿔도 옛 동화의 표지는 그대로다.
 *
 * <h2>왜 그림을 저장하지 않나</h2>
 * 삽화는 LLM 이 만든 것이 아니라 앱 번들에 들어 있는 정적 그림이고, 장면의
 * {@code category} 하나로 결정된다(Story.tsx 의 ILLUSTRATION 표). 즉 category 만
 * 있으면 화면이 그대로 재현된다. 이미지 바이트를 DB 에 넣으면 앨범 한 편에 수 MB 를
 * 쓰면서 얻는 것이 없고, 나중에 그림을 고쳐도 옛 앨범만 낡은 그림으로 남는다.
 *
 * <h2>왜 장면을 JSON 한 덩어리로 두나</h2>
 * 장면은 <b>동화와 함께 태어나 함께 읽힌다.</b> 장면만 따로 조회하거나 수정할 일이
 * 없고, 검색 조건이 되지도 않는다. 표를 나누면 조인만 늘고 얻는 것이 없다.
 * 스키마는 추론 서버가 만든 응답 모양을 그대로 따르므로, 그쪽이 바뀌어도 이 표는
 * 그대로 둔 채 앱이 읽는 방식만 맞추면 된다.
 */
@Entity
@Table(
        name = "story_album",
        indexes = {
            // 앨범 목록은 언제나 "이 아이의 것을 최신순으로"다. 두 칼럼을 함께 태운다.
            @Index(name = "idx_album_child_created", columnList = "child_id, created_at")
        })
public class StoryAlbum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기기가 만든 UUID. 로그인이 없으므로 이것이 "누구인가"의 전부다. */
    @Column(name = "child_id", nullable = false, length = 36)
    private String childId;

    /** 동화를 만든 그 시점의 이름. 나중에 이름을 바꿔도 옛 표지는 그대로 둔다. */
    @Column(name = "nickname", nullable = false, length = 40)
    private String nickname;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    /**
     * 장면 배열 원문(JSON).
     *
     * 추론 서버가 만든 모양을 그대로 담는다 — 게이트웨이가 뜻을 해석하지 않으므로
     * 스키마가 늘어나도 이 칼럼은 손댈 일이 없다.
     *
     * 칼럼 타입을 두 번 잘못 잡았고, 둘 다 실제로 깨졌다.
     *
     * <ul>
     *   <li><b>JSON 으로 잡으면</b> 문자열이 다시 JSON 문자열로 감싸여, 읽을 때
     *       배열이 아니라 따옴표 붙은 문자열이 돌아온다.
     *   <li><b>{@code @Lob} 만 붙이면</b> 길이를 안 줬다고 MySQL 에서 TINYTEXT
     *       (255바이트)가 된다. 동화 한 편의 장면 배열은 몇 KB 라 그대로 잘린다.
     *       메모리 DB(H2)에는 그 제한이 없어 테스트만으로는 드러나지 않는다.
     * </ul>
     *
     * 그래서 타입을 손으로 못박는다. 안을 검색할 일이 없으니 긴 텍스트면 충분하다.
     */
    @Column(name = "scenes_json", nullable = false, columnDefinition = "LONGTEXT")
    private String scenesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StoryAlbum() {
        // JPA 용
    }

    public StoryAlbum(String childId, String nickname, String title, String scenesJson, Instant createdAt) {
        this.childId = childId;
        this.nickname = nickname;
        this.title = title;
        this.scenesJson = scenesJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getChildId() {
        return childId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getTitle() {
        return title;
    }

    public String getScenesJson() {
        return scenesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
