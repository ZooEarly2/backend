package com.zooearly.album;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 동화 앨범 계약.
 *
 * 이 표는 <b>아이가 만든 것</b>을 담는다. 그래서 확인하는 것도 두 가지다 —
 * 넣은 그대로 다시 나오는가, 그리고 <b>남의 것이 보이지 않는가</b>.
 * 로그인이 없어 childId 가 유일한 열쇠이므로 뒤쪽이 특히 중요하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlbumApiTest {

    private static final String CHILD_A = "11111111-2222-3333-4444-555555555555";
    private static final String CHILD_B = "99999999-8888-7777-6666-555555555555";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static String body(String childId, String title) {
        return
                """
                {
                  "childId": "%s",
                  "nickname": "지우",
                  "title": "%s",
                  "scenes": [
                    {"category":"school_arrival","subtitle":"학교 오는 길","opening":"아침에",
                     "quote":"안녕! 우리 같이 놀자!","narration":"지우가 학교에 왔어요."},
                    {"category":"class","subtitle":"국어 시간","opening":"교실에서",
                     "quote":null,"narration":"동시를 읽었어요."}
                  ]
                }
                """
                        .formatted(childId, title);
    }

    private long save(String childId, String title) throws Exception {
        String json = mvc.perform(post("/api/v1/albums").contentType(MediaType.APPLICATION_JSON).content(body(childId, title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return mapper.readTree(json).path("data").path("id").asLong();
    }

    @Test
    @DisplayName("수업시간의 과목이 앨범에 남는다 — 삽화가 이 값으로 갈린다")
    void keepsClassSubject() throws Exception {
        String json =
                """
                {
                  "childId": "%s",
                  "nickname": "지우",
                  "title": "수학을 한 날",
                  "scenes": [
                    {"category":"class","classSubject":"MATH","subtitle":"수학 시간",
                     "opening":"교실에서","quote":null,"narration":"사과 세 개를 세었어요."}
                  ]
                }
                """
                        .formatted(CHILD_A);
        String saved = mvc.perform(post("/api/v1/albums").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = mapper.readTree(saved).path("data").path("id").asLong();

        String read = mvc.perform(get("/api/v1/albums/" + id).param("childId", CHILD_A))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode scene = mapper.readTree(read).path("data").path("scenes").get(0);

        // 이 값이 빠지면 앱이 과일을 센 아이에게 "동시 읽어보기" 그림을 보여준다.
        // 방금 만든 동화는 맞게 나와도 앨범에서 다시 꺼내면 틀린다 — 그 자리를 막는다.
        assert scene.path("classSubject").asText().equals("MATH") : read;
    }

    @Test
    @DisplayName("남긴 동화가 그대로 다시 나온다 — 화면을 재현할 수 있어야 한다")
    void savesAndReadsBack() throws Exception {
        long id = save(CHILD_A, "지우의 하루");

        String json = mvc.perform(get("/api/v1/albums/{id}", id).param("childId", CHILD_A))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = mapper.readTree(json).path("data");
        assert data.path("title").asText().equals("지우의 하루");
        assert data.path("nickname").asText().equals("지우");
        JsonNode scenes = data.path("scenes");
        assert scenes.size() == 2;
        // 삽화를 고르는 유일한 키다. 이게 없으면 같은 그림을 다시 못 그린다.
        assert scenes.get(0).path("category").asText().equals("school_arrival");
        // 아이가 실제로 한 말. 화면에 인용 블록으로 뜬다.
        assert scenes.get(0).path("quote").asText().equals("안녕! 우리 같이 놀자!");
        assert scenes.get(1).path("subtitle").asText().equals("국어 시간");
    }

    @Test
    @DisplayName("목록은 내 것만 보인다")
    void listIsScopedToChild() throws Exception {
        save(CHILD_A, "A 의 동화");
        save(CHILD_B, "B 의 동화");

        mvc.perform(get("/api/v1/albums").param("childId", CHILD_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("B 의 동화"))
                // 목록에서는 보석 줄만 그리면 되므로 장면 종류가 함께 온다
                .andExpect(jsonPath("$.data[0].categories[0]").value("school_arrival"));
    }

    @Test
    @DisplayName("번호를 알아도 남의 동화는 못 연다 — 인증이 없어 childId 가 유일한 열쇠다")
    void cannotReadAnotherChildsStory() throws Exception {
        long mine = save(CHILD_A, "내 동화");

        mvc.perform(get("/api/v1/albums/{id}", mine).param("childId", CHILD_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("childId 가 UUID 가 아니면 거절한다")
    void rejectsNonUuidChildId() throws Exception {
        mvc.perform(get("/api/v1/albums").param("childId", "지우"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("childId"));

        mvc.perform(post("/api/v1/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-a-uuid", "제목")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("긴 동화도 잘리지 않는다 — 칼럼이 TINYTEXT 로 잡혀 실제로 잘린 적이 있다")
    void keepsLongStoryIntact() throws Exception {
        // 실제 동화 한 편의 장면 배열은 몇 KB 다. 칼럼을 LONGTEXT 로 못박기 전에는
        // MySQL 에서 TINYTEXT(255바이트)가 되어 그대로 잘렸다.
        String narration = "노란 꽃이 피었어요. ".repeat(120); // 2천 자 남짓
        String longBody = body(CHILD_A, "긴 동화").replace("지우가 학교에 왔어요.", narration);

        String json = mvc.perform(post("/api/v1/albums").contentType(MediaType.APPLICATION_JSON).content(longBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = mapper.readTree(json).path("data").path("id").asLong();

        String read = mvc.perform(get("/api/v1/albums/{id}", id).param("childId", CHILD_A))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String back = mapper.readTree(read).path("data").path("scenes").get(0).path("narration").asText();
        assert back.equals(narration) : "내레이션이 잘렸다: " + back.length() + " / " + narration.length();
    }

    @Test
    @DisplayName("알 수 없는 장면 종류는 거절한다 — 그리면 삽화를 못 고른다")
    void rejectsUnknownCategory() throws Exception {
        String bad = body(CHILD_A, "제목").replace("school_arrival", "playground");
        mvc.perform(post("/api/v1/albums").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest());
    }
}
