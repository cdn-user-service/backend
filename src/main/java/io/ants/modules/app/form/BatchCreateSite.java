package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchCreateSite {

  @NotNull(message = "userId not be null")
  private Long userId;

  @NotNull(message = "serialNumber not be null")
  private String serialNumber;

  @NotNull(message = "sProtocol not be null")
  private String sProtocol;

  @NotNull(message = "serverSource not be null")
  private List<String> serverSource;

  private int groupId;
}
