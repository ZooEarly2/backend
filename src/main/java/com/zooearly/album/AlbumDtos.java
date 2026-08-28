package com.zooearly.album;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 앨범 API 가 주고받는 모양.
 *
 * 이 계층은 릴레이가 아니라 게이트웨이의 <b>자기 도메인</b>이라, 여기서는 body 를
 * 열어 본다. 릴레이(/api/v1/ai/*)가 body 를 안 여는 것과 다른 이유다 —
 * 그쪽은 추론 서버의 계약을 그대로 통과시키는 일이고, 이쪽은 우리가 계약의 주인이다.
 */
public final class AlbumDtos {

    private AlbumDtos() {}

    /** 앱이 만든 UUID. 로그인이 없으므로 이 값이 곧 열쇠다 — 모양을 엄격히 본다. */
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public record SaveRequest(
            @NotBlank(message = "childId 가 필요합니다.")
                    @Pattern(regexp = UUID_PATTERN, message = "childId 는 UUID 여야 합니다.")
                    String childId,
            @NotBlank(message = "nickname 이 필요합니다.") @Size(max = 40) String nickname,
            @NotBlank(message = "title 이 필요합니다.") @Size(max = 120) String title,
            @NotEmpty(message = "scenes 가 비어 있습니다.") @Valid List<Scene> scenes) {}

    /**
     * 장면 하나.
     *
     * 삽화는 담지 않는다 — 앱 번들의 정적 그림이고 {@code category} 와
     * {@code classSubject} 로 결정된다. 그림 바이트를 저장하면 동화마다 같은 파일을
     * 복제하면서, 나중에 그림을 고쳐도 옛 앨범만 낡은 그림으로 남는다.
     */
    public record Scene(
            @NotBlank @Pattern(
                            regexp = "school_arrival|class|lunch|school_departure",
                            message = "알 수 없는 장면 종류입니다.")
                    String category,
            @NotBlank @Size(max = 120) String subtitle,
            @Size(max = 2000) String opening,
            @Size(max = 500) String quote,
            @NotBlank @Size(max = 4000) String narration,
            /**
             * 수업시간의 과목. 수업 장면이 아니면 {@code null} 이다.
             *
             * <p><b>삽화가 이 값으로 갈린다.</b> 그림 안에 "국어시간 · 동시 읽어보기" 가
             * 글자로 그려져 있어서, 없으면 과일을 센 아이가 동시를 읽은 그림을 받는다.
             *
             * <p>여기 담지 않으면 앨범에 저장될 때 값이 버려져서, 방금 만든 동화는
             * 맞게 나와도 <b>나중에 다시 꺼내 보면 또 국어 그림</b>이 된다.
             *
             * <p>옛 앨범에는 이 값이 없다({@code null}). 그때는 수업시간이 동시 읽기
             * 하나뿐이었으므로 앱이 국어로 보는 것이 사실에 맞는다.
             */
            @Pattern(regexp = "KOREAN|MATH", message = "알 수 없는 과목입니다.")
                    String classSubject) {}

    /** 목록에 쓰는 요약. 표지를 그리는 데 필요한 만큼만 담는다. */
    public record Summary(long id, String title, String nickname, String createdAt, List<String> categories) {}

    /** 한 편 전체. 앱이 이걸 받아 동화 화면을 그대로 다시 그린다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(long id, String title, String nickname, String createdAt, List<Scene> scenes) {}

    public record SaveResult(long id) {}
}
