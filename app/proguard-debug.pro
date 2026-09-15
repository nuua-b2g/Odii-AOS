# debug 빌드 전용 R8 규칙
# release 는 Google Play DEX 코드 최적화 요구사항(축소/난독화/최적화 각 25% 이상) 때문에 최적화를 켜야 하지만,
# debug 에서는 인라이닝 때문에 브레이크포인트/스텝 디버깅이 불편해지므로 최적화만 끈다.
-dontoptimize
