package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class BatchModifySiteAttrForm {

    @Data
    public static  class KvObj {
        @NotNull
        private String key;

        private String value;
    }

    @NotNull
    private String siteIds;


    private List<KvObj> list;


}
