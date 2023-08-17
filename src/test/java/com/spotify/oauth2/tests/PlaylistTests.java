package com.spotify.oauth2.tests;

import com.spotify.oauth2.api.StatusCode;
import com.spotify.oauth2.api.applicationApi.PlaylistApi;
import com.spotify.oauth2.pojo.Error;
import com.spotify.oauth2.pojo.Playlist;

import com.spotify.oauth2.utils.DataLoader;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.spotify.oauth2.utils.FakerUtils.generateDescription;
import static com.spotify.oauth2.utils.FakerUtils.generateName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Epic("Spotify OAuth 2.0")
@Feature("Playlist API")
public class PlaylistTests extends BaseTest {

    @Step
    public Playlist playlistBuilder(String name, String description, boolean _public) {
        return Playlist.builder().
        name(name).
        description(description).
        _public(_public).
        build();
    }

    @Step
    public void assertPlaylistEqual(Playlist responsePlaylist, Playlist requestPlaylist) {
        assertThat(responsePlaylist.getName(), equalTo(requestPlaylist.getName()));
        assertThat(responsePlaylist.getDescription(), equalTo(requestPlaylist.getDescription()));
        assertThat(responsePlaylist.get_public(), equalTo(requestPlaylist.get_public()));
    }

    @Step
    public void assertStatusCode(int actualStatusCode, StatusCode expectedStatusCode) {
        assertThat(actualStatusCode, equalTo(expectedStatusCode.getCode()));
    }

    @Step
    public void assertError(Error responseErr, StatusCode expectedStatusCode, StatusCode expectedMsg) {
        assertThat(responseErr.getInnerError().getStatus(), equalTo(expectedStatusCode.getCode()));
        assertThat(responseErr.getInnerError().getMessage(), equalTo(expectedMsg.getMessage()));
    }

    @Story("Create a playlist story")
    @Link("https://example.org")
    @Link(name = "allure", type = "mylink")
    @TmsLink("12345")
    @Issue("1234567")
    @Description("Should be able to create Playlist with correct name, description and public status")
    @Test(description = " Should be able to create Playlist")
    void shouldBeAvailableToCreatePlaylist() {
        // Create playlist object
        Playlist requestPlaylist = playlistBuilder(generateName(),
        generateDescription(), false);

        // Get response and assert status code
        Response response = PlaylistApi.post(requestPlaylist);
        assertStatusCode(response.statusCode(), StatusCode.CODE_201);

        // Deserialize response to Playlist object
        Playlist responsePlaylist = response.as(Playlist.class);

        // Assert Playlist object
        assertPlaylistEqual(responsePlaylist, requestPlaylist);
    }

    @Description("Should be able to get Playlist with correct name, description and public status")
    @Test(description = "Should be able to get Playlist")
    void shouldBeAvailableToGetPlaylist() {
        Playlist requestPlaylist = playlistBuilder("Updated Playlist Name",
        "Updated playlist description", false);

        Response response = PlaylistApi.get(DataLoader.getInstance().getPlaylistId());
        assertStatusCode(response.statusCode(), StatusCode.CODE_200);

        Playlist responsePlaylist = response.as(Playlist.class);
        assertPlaylistEqual(responsePlaylist, requestPlaylist);
    }

    @Description("Should be able to update Playlist with correct name, description and public status")
    @Test(description = "Should be able to update Playlist")
    void shouldBeAvailableToUpdatePlaylist() {
        Playlist requestPlaylist = playlistBuilder(generateName(),
        generateName(), false);

        Response response = PlaylistApi.update(requestPlaylist, DataLoader.getInstance().getUpdatePlaylistId());
        assertStatusCode(response.statusCode(), StatusCode.CODE_200);
    }

    @Story("Create a playlist story")
    @Test(description = "Should not be able to create Playlist without name")
    void shouldNotBeAvailableToCreatePlaylistWithoutName() {
        Playlist requestPlaylist = playlistBuilder("",
        generateDescription(), false);

        Response response = PlaylistApi.post(requestPlaylist);
        assertStatusCode(response.statusCode(), StatusCode.CODE_400);
        assertError(response.as(Error.class), StatusCode.CODE_400, StatusCode.CODE_400);
    }

    @Story("Create a playlist story")
    @Test(description = "Should not be able to create Playlist with expired token")
    void shouldNotBeAvailableToCreatePlaylistWithExpiredToken() {
        String invalid_token = "123";

        Playlist requestPlaylist = playlistBuilder(generateName(),
        generateDescription(), false);

        Response response = PlaylistApi.post(requestPlaylist, invalid_token);
        assertStatusCode(response.statusCode(), StatusCode.CODE_401);
        assertError(response.as(Error.class), StatusCode.CODE_401, StatusCode.CODE_401);
    }
}
