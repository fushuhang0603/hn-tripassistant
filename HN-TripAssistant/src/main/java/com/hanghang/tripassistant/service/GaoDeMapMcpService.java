package com.hanghang.tripassistant.service;

/**
 * 高德地图 MCP 工具封装接口。
 * 各方法均返回高德工具返回的原始 JSON 文本，由调用方按需解析。
 */
public interface GaoDeMapMcpService {

    /**
     * 天气查询（maps_weather）。
     *
     * @param city 城市名称或城市 adcode，如 "三亚"
     */
    String weather(String city);


    /**
     * 地理编码（maps_geo）：地址 → 经纬度。
     *
     * @param address 结构化地址，如 "三亚市亚龙湾"
     * @param city    城市信息（可选）
     */
    String geo(String address, String city);

    /**
     * 关键词搜索（maps_text_search）。
     *
     * @param keywords 搜索关键词，如 "天涯海角"
     * @param city     查询城市（可选，城市中文/citycode/adcode）
     */
    String textSearch(String keywords, String city);

    /**
     * 周边搜索（maps_around_search）。
     *
     * @param location 中心点经纬度 "lon,lat"
     * @param keywords 周边搜索关键词
     */
    String aroundSearch(String location, String keywords);

    /**
     * POI 详情（maps_search_detail）。
     *
     * @param id POI 唯一 id
     */
    String searchDetail(String id);

    /**
     * 距离测量（maps_distance）。
     *
     * @param origin      起点经纬度 "lon,lat"
     * @param destination 终点经纬度 "lon,lat"
     */
    String distance(String origin, String destination);

    /**
     * 驾车路径规划（maps_direction_driving）。
     *
     * @param origin      起点经纬度 "lon,lat"
     * @param destination 终点经纬度 "lon,lat"
     */
    String directionDriving(String origin, String destination);

    /**
     * 步行路径规划（maps_direction_walking）。
     *
     * @param origin      起点经纬度 "lon,lat"
     * @param destination 终点经纬度 "lon,lat"
     */
    String directionWalking(String origin, String destination);

    /**
     * 公交路径规划（maps_direction_transit_integrated）。
     *
     * @param origin      起点经纬度 "lon,lat"
     * @param destination 终点经纬度 "lon,lat"
     * @param city        起点城市（跨城场景必传）
     * @param cityd       终点城市（跨城场景必传）
     */
    String directionTransit(String origin, String destination, String city, String cityd);
}
