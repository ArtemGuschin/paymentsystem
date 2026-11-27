
-- Разрешаем использование стандартных тестовых
-- способов оплаты Fake Provider для всех стран.
--
-- Меняем только общие определения CARD и SBP.
-- Специальные настройки для отдельных стран
-- остаются без изменений.

UPDATE payment_method_definitions d
SET is_all_countries = TRUE
    FROM payment_methods pm
JOIN payment_providers p
ON p.id = pm.provider_id
WHERE d.payment_method_id = pm.id
  AND p.name = 'FAKE'
  AND pm.type IN ('CARD', 'SBP')
  AND d.is_active = TRUE
  AND d.is_all_currencies = TRUE
  AND d.currency_code IS NULL
  AND d.country_alpha3_code IS NULL
  AND d.is_all_countries = FALSE;
