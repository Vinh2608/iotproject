from yolobit import *
button_a.on_pressed = None
button_b.on_pressed = None
button_a.on_pressed_ab = button_b.on_pressed_ab = -1
from event_manager import *
import time
from machine import Pin, SoftI2C
from aiot_dht20 import DHT20
import sys
import uselect

event_manager.reset()

aiot_dht20 = DHT20(SoftI2C(scl=Pin(22), sda=Pin(21)))

def on_event_timer_callback_r_o_e_g_j():
  global cmd, th_C3_B4ng_tin
  aiot_dht20.read_dht20()
  print((''.join([str(x) for x in ['!t:', aiot_dht20.dht20_temperature(), '#']])), end =' ')
  print((''.join([str(x2) for x2 in ['!as:', light_level(), '#']])), end =' ')

event_manager.add_timer_event(15000, on_event_timer_callback_r_o_e_g_j)

def read_terminal_input():
  spoll=uselect.poll()        # Set up an input polling object.
  spoll.register(sys.stdin, uselect.POLLIN)    # Register polling object.

  input = ''
  if spoll.poll(0):
    input = sys.stdin.read(1)

    while spoll.poll(0):
      input = input + sys.stdin.read(1)

  spoll.unregister(sys.stdin)
  return input

def on_event_timer_callback_H_D_h_h_m():
  global cmd, th_C3_B4ng_tin
  cmd = read_terminal_input()
  number = [0, 25, 50, 75, 100]
  
  print(cmd, end =' ')
  if cmd == 'L':
    print('Light', end =' ')
    pin2.write_digital(1)
  if cmd == 'l':
    print('light', end =' ')
    pin2.write_digital(0)
  # if cmd == 'F':
  #   print('Fan', end =' ')
  #   pin1.write_analog(round(translate(100, 0, 100, 0, 1023)))
  if cmd in number:
    print(f'fan {cmd}', end =' ')
    pin1.write_analog(round(translate(int(cmd), 0, 100, 0, 1023)))

event_manager.add_timer_event(500, on_event_timer_callback_H_D_h_h_m)

if True:
  display.scroll('ON')

while True:
  event_manager.run()
  time.sleep_ms(1000)
