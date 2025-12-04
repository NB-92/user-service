package com.prpo.chat.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "users")
public class User {

  @Id
  private String id;

  private String username;
  private String email;
  private String passwordHash;

  private Profile profile;
  private Settings settings;

  private List<String> friends; // list of user ids

  @Data
  public static class Profile {
    private String avatarUrl;
    private String bio;
    private String location;
    private String birthdate;
  }

  @Data
  public static class Settings {
    private Theme theme = Theme.DARK;
    private boolean notifications;
  }

  public enum Theme {
    DARK,
    LIGHT;
  }

}
