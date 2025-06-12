package io.ants.modules.app.form;


import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value = "余额支付")
public class PayBalanceForm {
   private String serialNumber;

   private String password;

   private Long  userId;

}
