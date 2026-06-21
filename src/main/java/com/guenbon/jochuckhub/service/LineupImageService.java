package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.response.MatchLineupResponse.PlayerAssignment;
import com.guenbon.jochuckhub.entity.Position;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 4-3-3 포메이션 라인업 이미지를 생성하는 서비스.
 *
 * 포메이션 슬롯 순서 (MatchLineupService.FORMATION 과 동일):
 * 0:LB  1:CB(좌)  2:CB(우)  3:RB
 * 4:CDM
 * 5:CM(좌)  6:CM(우)
 * 7:LW  8:ST  9:RW
 */
@Service
@Slf4j
public class LineupImageService {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final int IMG_W = 600;
    private static final int IMG_H = 900;
    private static final int HEADER_H = 115;

    // 각 포메이션 슬롯의 중심 좌표 [x, y]
    private static final int[][] SLOT_XY = {
        { 70, 810},  // 0: LB
        {200, 835},  // 1: CB (좌)
        {400, 835},  // 2: CB (우)
        {530, 810},  // 3: RB
        {300, 615},  // 4: CDM
        {155, 565},  // 5: CM (좌)
        {445, 565},  // 6: CM (우)
        {110, 315},  // 7: LW
        {300, 280},  // 8: ST
        {490, 315},  // 9: RW
    };

    // 포지션 → 첫 슬롯 인덱스 (중복 포지션은 인접 인덱스 사용: CB=1,2 / CM=5,6)
    private static final Map<Position, Integer> POSITION_BASE;
    static {
        POSITION_BASE = new EnumMap<>(Position.class);
        POSITION_BASE.put(Position.LB,  0);
        POSITION_BASE.put(Position.CB,  1);
        POSITION_BASE.put(Position.RB,  3);
        POSITION_BASE.put(Position.CDM, 4);
        POSITION_BASE.put(Position.CM,  5);
        POSITION_BASE.put(Position.LW,  7);
        POSITION_BASE.put(Position.ST,  8);
        POSITION_BASE.put(Position.RW,  9);
    }

    private static final Color COLOR_MAIN  = new Color(25,  100, 210);
    private static final Color COLOR_SUB   = new Color(200, 140,  20);
    private static final Color COLOR_OTHER = new Color(180,  45,  45);
    private static final Color FIELD_GREEN = new Color(34,  139,  34);
    private static final Color FIELD_STRIPE = new Color(30,  130,  30);

    /**
     * 단일 쿼터의 라인업 이미지를 PNG byte[] 로 반환한다.
     */
    public byte[] generateQuarterImage(int quarter, List<PlayerAssignment> players,
                                       String matchTitle) throws IOException {
        BufferedImage img = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHeader(g, quarter, matchTitle);
        drawField(g);
        drawPlayers(g, players);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    // ────────────────────────────────────────────────

    private void drawHeader(Graphics2D g, int quarter, String matchTitle) {
        g.setColor(new Color(20, 25, 45));
        g.fillRect(0, 0, IMG_W, HEADER_H);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
        String title = quarter + "쿼터 라인업";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (IMG_W - fm.stringWidth(title)) / 2, 52);

        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
        fm = g.getFontMetrics();
        g.setColor(new Color(170, 175, 200));
        g.drawString(matchTitle, (IMG_W - fm.stringWidth(matchTitle)) / 2, 85);
    }

    private void drawField(Graphics2D g) {
        // 기본 초록 배경
        g.setColor(FIELD_GREEN);
        g.fillRect(0, HEADER_H, IMG_W, IMG_H - HEADER_H);

        // 줄무늬
        g.setColor(FIELD_STRIPE);
        for (int y = HEADER_H; y < IMG_H; y += 50) {
            g.fillRect(0, y, IMG_W, 25);
        }

        int mx = 22, my = HEADER_H + 18;
        int fw = IMG_W - 2 * mx;
        int fh = IMG_H - HEADER_H - 36;
        int cy = my + fh / 2;

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));

        // 필드 경계
        g.drawRect(mx, my, fw, fh);
        // 중앙선
        g.drawLine(mx, cy, mx + fw, cy);
        // 중앙 원
        int cr = 58;
        g.drawOval(IMG_W / 2 - cr, cy - cr, cr * 2, cr * 2);
        // 중앙 점
        g.fillOval(IMG_W / 2 - 4, cy - 4, 8, 8);

        // 상단 페널티 박스 (상대팀 골대)
        int pbw = 240, pbh = 85;
        g.drawRect((IMG_W - pbw) / 2, my, pbw, pbh);
        // 하단 페널티 박스 (홈팀 골대)
        g.drawRect((IMG_W - pbw) / 2, my + fh - pbh, pbw, pbh);

        // 공격 방향 표시
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        g.setColor(new Color(255, 255, 255, 150));
        String arrow = "▲ 공격 방향";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(arrow, IMG_W - mx - fm.stringWidth(arrow), my + 22);
    }

    private void drawPlayers(Graphics2D g, List<PlayerAssignment> players) {
        Map<Position, Integer> placed = new EnumMap<>(Position.class);
        for (PlayerAssignment player : players) {
            Position pos = player.getAssignedPosition();
            Integer base = POSITION_BASE.get(pos);
            if (base == null) {
                log.warn("알 수 없는 포지션 슬롯: {}", pos);
                continue;
            }
            int offset = placed.getOrDefault(pos, 0);
            int idx = base + offset;
            placed.put(pos, offset + 1);
            if (idx >= SLOT_XY.length) continue;
            drawToken(g, SLOT_XY[idx][0], SLOT_XY[idx][1], player);
        }
    }

    private void drawToken(Graphics2D g, int cx, int cy, PlayerAssignment player) {
        int r = 28;
        Color fill = switch (player.getPositionFit()) {
            case "MAIN" -> COLOR_MAIN;
            case "SUB"  -> COLOR_SUB;
            default     -> COLOR_OTHER;
        };

        // 원형 마커
        g.setColor(fill);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);

        // 포지션 약어 (원 안)
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        String posText = player.getAssignedPosition().name();
        g.setColor(Color.WHITE);
        g.drawString(posText, cx - fm.stringWidth(posText) / 2, cy + fm.getAscent() / 2 - 2);

        // 이름 배지 (원 아래)
        g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        fm = g.getFontMetrics();
        String name = player.getMemberName();
        if (name.length() > 6) name = name.substring(0, 6);
        int nw = fm.stringWidth(name) + 10;
        int nh = fm.getHeight() + 2;
        int nx = cx - nw / 2;
        int ny = cy + r + 4;

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(nx, ny, nw, nh, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(name, nx + 5, ny + fm.getAscent() + 1);
    }
}
