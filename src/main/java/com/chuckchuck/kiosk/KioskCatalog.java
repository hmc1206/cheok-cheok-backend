package com.chuckchuck.kiosk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class KioskCatalog {
    private final Map<String, KioskScenario> scenarios;

    public KioskCatalog() {
        scenarios = new LinkedHashMap<>();
        scenarios.put("kiosk_cafe", cafeScenario());
        scenarios.put("kiosk_fastfood", fastFoodScenario());
    }

    public List<KioskScenario> findAll() {
        return List.copyOf(scenarios.values());
    }

    public Optional<KioskScenario> findById(String scenarioId) {
        return Optional.ofNullable(scenarios.get(scenarioId));
    }

    private KioskScenario cafeScenario() {
        String base = "https://cdn.chuckchuck.com/kiosk/cafe/";
        return new KioskScenario(
                "kiosk_cafe",
                "카페 키오스크 연습",
                base + "thumbnail.png",
                "EASY",
                List.of(
                        step(base, 1, "화면 아래 주문하기 버튼을 눌러보세요.", "btn_order", 120, 480),
                        step(base, 2, "이번에는 아메리카노를 눌러보세요.", "menu_americano", 90, 220),
                        step(base, 3, "따뜻한 음료를 선택해 보세요.", "option_hot", 80, 320),
                        step(base, 4, "카드 결제 버튼을 눌러보세요.", "pay_card", 120, 410),
                        step(base, 5, "마지막으로 결제하기 버튼을 눌러보세요.", "btn_pay", 120, 500)
                )
        );
    }

    private KioskScenario fastFoodScenario() {
        String base = "https://cdn.chuckchuck.com/kiosk/fastfood/";
        return new KioskScenario(
                "kiosk_fastfood",
                "패스트푸드 키오스크 연습",
                base + "thumbnail.png",
                "MEDIUM",
                List.of(
                        step(base, 1, "화면 아래 주문하기 버튼을 눌러보세요.", "btn_order", 120, 480),
                        step(base, 2, "햄버거 메뉴를 눌러보세요.", "menu_burger", 90, 220),
                        step(base, 3, "단품으로 주문하기를 눌러보세요.", "option_single", 80, 320),
                        step(base, 4, "매장에서 먹기 버튼을 눌러보세요.", "eat_here", 100, 410),
                        step(base, 5, "카드 결제 버튼을 눌러보세요.", "pay_card", 120, 500)
                )
        );
    }

    private KioskStep step(
            String base,
            int index,
            String guide,
            String elementId,
            int x,
            int y
    ) {
        return new KioskStep(
                base + "step-" + index + ".png",
                guide,
                guide,
                List.of(new TapTarget(elementId, x, y, 200, 60)),
                elementId,
                "다시 한번 해볼까요? " + guide,
                base + "hint-" + index + ".png"
        );
    }
}
