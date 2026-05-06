package com.fence.util;

import com.fence.entity.FenceVertex;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 临时碰撞检测工具类
 * TODO: 后续由成员A实现更精确的算法
 */
@Slf4j
public class CollisionUtil {

    /**
     * 判断点是否在多边形内（射线法）
     *
     * @param pointLongitude 点的经度
     * @param pointLatitude  点的纬度
     * @param vertices       多边形顶点列表（按顺序）
     * @return true-在多边形内，false-在多边形外
     */
    public static boolean isPointInPolygon(Double pointLongitude, Double pointLatitude,
                                           List<FenceVertex> vertices) {
        if (vertices == null || vertices.size() < 3) {
            log.warn("围栏顶点数量不足，无法进行碰撞检测");
            return false;
        }

        int n = vertices.size();
        boolean inside = false;

        double x = pointLongitude;
        double y = pointLatitude;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = vertices.get(i).getLongitude();
            double yi = vertices.get(i).getLatitude();
            double xj = vertices.get(j).getLongitude();
            double yj = vertices.get(j).getLatitude();

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        log.debug("碰撞检测结果: point=({}, {}), inside={}", x, y, inside);
        return inside;
    }

    /**
     * 计算两点之间的距离（Haversine公式）
     *
     * @param lon1 点1经度
     * @param lat1 点1纬度
     * @param lon2 点2经度
     * @param lat2 点2纬度
     * @return 距离（米）
     */
    public static double calculateDistance(Double lon1, Double lat1,
                                           Double lon2, Double lat2) {
        final int R = 6371000; // 地球半径（米）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
