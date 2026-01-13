package com.artem.transactionservice;

import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class HintAlgorithm implements HintShardingAlgorithm<Long> {
    private static final Logger log = LoggerFactory.getLogger(HintAlgorithm.class);

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, HintShardingValue<Long> hintShardingValue) {

        // Проверяем, что значения для шардирования предоставлены
        if (hintShardingValue.getValues().isEmpty()) {
            throw new IllegalArgumentException("Не найдено шард в подсказке");
        }

        // Получаем ключ шардирования
        Long shardingKey = hintShardingValue.getValues().iterator().next();
        log.debug("shardingKey: {}", shardingKey);

        // Формируем имя шарда
        String selectedShard = "ds_" + shardingKey;
        log.debug("selectedShard: {}", selectedShard);

        // Проверяем, что выбранный шард существует среди доступных целей
        if (!availableTargetNames.contains(selectedShard)) {
            throw new IllegalArgumentException("Selected shard '" + selectedShard + "' is not available.");
        }

        return Collections.singletonList(selectedShard);
    }

    @Override
    public String getType() {
        return "HINT";
    }

}
