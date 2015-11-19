import sys

from pymrgeo import MrGeo
from pymrgeo.rastermapop import RasterMapOp

if __name__ == "__main__":
    mrgeo = MrGeo()

    #mrgeo.useDebug()

    mrgeo.start()

    #ones = mrgeo.load_resource("all-ones")

    #slope = ones.slope()

    #hundreds = mrgeo.load_resource("all-hundreds")
    #aspect = hundreds.aspect()

    #slope.save("slope-test")
    #aspect.save("aspect-test")

    small_elevation = mrgeo.load_resource("small-elevation")
    slope = small_elevation.slope()
    slope.save("slope-test")

    mrgeo.stop()