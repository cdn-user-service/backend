package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class GetTextParamVo {
    // //{"variableName":"###nft_long_cc###","variableVersion":"2.17","client":{"area":"1","createtime":1660466058000,"line":"中国移动","stableScore":48627,"remark":"1","regInfo":"{\"addtime\":\"1660466070\",\"ip\":\"119.97.137.47\",\"goods\":\"cdn节点\",\"endtime\":\"1862064000\",\"version\":\"2.17\"}","version":"2.17","confInfo":"{\"id\":84,\"proxy_cache_dir_max_size\":\"100G\",\"proxy_max_temp_file_size\":\"1024m\",\"proxy_cache_path_zone\":\"1000m\",\"worker_processes\":\"auto\",\"proxy_cache_path_dir\":\"/data/cache\",\"error_log_level\":\"error\"}","effectiveStartTime":1660466070000,"clientType":1,"checkTime":1666234207,"clientIp":"119.97.137.47","id":84,"updatetime":1666234207000,"effectiveEndingTime":1862064000000,"status":1}}
    private String variableName;

    private String variableVersion;

    private String clientObjJsonString;
}
