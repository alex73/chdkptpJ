This is java framework for access to CHDK cameras.

External dependencies:

1. It uses camera-side and desktop-side Lua code from chdkptp and executes using luaj for some complex things like remote shoot: https://www.assembla.com/spaces/chdkptp/. Scripts from chdktpt are stored in the lua-orig/ directory.
2. It uses usb4java with native libraries and can require to additional configuration in Linux or Windows. See http://usb4java.org/faq.html

Thanks to the CHDK-PTP-Java project for ability to understand how to call cameras from java so simple. I planned to use this project first, but looks like it's not in active development anymore. I hope my implementation is much simpler.

Thanks to the CHDK team for creating software for control cameras, and especially to the reyalp for the chdkptp and advices.
