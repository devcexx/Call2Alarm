# Call2Alarm

## What is it?

Call2Alarm is an app that will listen for incoming calls through a foreground service, and it will
play a ringtone through the alarm channel when that happens. This is just an app I did for my
own self, an for such reason decisions like targeting just Android 13 has been taken. 

## Who the hell would need something like this?

Me. And potentially OnePlus users that intensively uses Do Not Disturb mode.

Since a couple of major Android versions (not exactly remember which one), OnePlus took the
fantastic decision of taking the incredibly awesome and complete Do Not Disturb mode from Google,
and intentionally make it worse and limit it for the interest of their users. One of the
features that the DND mode of OnePlus removes is the ability to block messages and other
notifications, but allow calls from anyone. For me, that if someone calls me is probably because
it is important, is an especially painful situation to deal with: Either you disable DND mode and
let that all the shit notifications to disturb you while working or sleeping; or you loose the
ability to hear phone calls.

After some experimentation, this issue seems to be related with the operating system and not with a
missing options in the system preferences, because even third parties apps are not able to modify
the notification policies properly to fix this behaviour.

The objective of this app is to work while DND is blocking all the sounds of incoming calls, and
play the ringtone through the alarm channel, that is not blocked by OnePlus DND, everytime a call
comes in, allowing you to hear it and respond even when the device is in DND mode. This app is
dummy and doesn't really know if the system will play the ringtone or not, so for now if the
app is configured while DND allows passing some calls (contacts, starred, or repeated calls), the
device will simply reproduce the ringtone twice. For that reason it is recommended to just use
it with a properly configured DND mode.
