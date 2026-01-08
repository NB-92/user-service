package com.prpo.chat.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "users")
public class User {

  @Id
  private String id;

  @Indexed(unique = true)
  private String email;
  private String username;
  private String passwordHash;

  @Indexed(unique = true, sparse = true)
  private String walletAddress;

  private Profile profile;
  private Settings settings;

  private List<String> friends; // list of user ids
  private List<String> pendingFriendshipRequests;

  @Data
  public static class Profile {
    private String avatarUrl;
    private String bio;
    private Date birthdate;
  }

  @Data
  public static class Settings {
    private Theme theme = Theme.DARK;
    private boolean notifications;
  }

  public enum Theme {
    DARK,
    LIGHT
  }

}
