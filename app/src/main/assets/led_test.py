if robot._ring_led:
    robot._ring_led.set_scenario(RingLed.ColorWheel)
    time.sleep(2)
    robot._ring_led.set_scenario(RingLed.Off)