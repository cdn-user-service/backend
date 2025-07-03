package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "余额支付")
public class PayBalanceForm {
   private String serialNumber;

   private String password;

   private Long userId;

}
