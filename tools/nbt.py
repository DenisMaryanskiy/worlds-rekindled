"""Minimal NBT read/write (gzipped, as Minecraft stores structure templates) with no dependencies.

Python values map to tags by type: bool and Byte -> byte, int -> int, float -> double, str -> string,
dict -> compound, list -> list (all of one type). Wrap a number in Byte, Short, Long or Float for the others.
"""
import gzip
import struct


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Float(float):
    pass


def _tag(value):
    if isinstance(value, (bool, Byte)):
        return 1
    if isinstance(value, Short):
        return 2
    if isinstance(value, Long):
        return 4
    if isinstance(value, int):
        return 3
    if isinstance(value, Float):
        return 5
    if isinstance(value, float):
        return 6
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError(type(value))


def _string(text):
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _payload(value):
    tag = _tag(value)
    if tag == 1:
        return struct.pack(">b", int(value))
    if tag == 2:
        return struct.pack(">h", value)
    if tag == 3:
        return struct.pack(">i", value)
    if tag == 4:
        return struct.pack(">q", value)
    if tag == 5:
        return struct.pack(">f", value)
    if tag == 6:
        return struct.pack(">d", value)
    if tag == 8:
        return _string(value)
    if tag == 9:
        kinds = {_tag(item) for item in value}
        assert len(kinds) <= 1, "a list holds one type of tag"
        kind = kinds.pop() if kinds else 0
        return struct.pack(">bi", kind, len(value)) + b"".join(_payload(item) for item in value)
    return b"".join(struct.pack(">b", _tag(item)) + _string(key) + _payload(item) for key, item in value.items()) + b"\x00"


def write(path, root):
    with gzip.GzipFile(path, "wb", mtime=0) as out:
        out.write(b"\x0a" + _string("") + _payload(root))


def read(path):
    data = gzip.open(path, "rb").read()
    pos = 0

    def take(fmt):
        nonlocal pos
        size = struct.calcsize(fmt)
        values = struct.unpack(fmt, data[pos:pos + size])
        pos += size
        return values[0]

    def string():
        nonlocal pos
        length = take(">H")
        text = data[pos:pos + length].decode("utf-8")
        pos += length
        return text

    def payload(tag):
        if tag == 1:
            return Byte(take(">b"))
        if tag == 2:
            return Short(take(">h"))
        if tag == 3:
            return take(">i")
        if tag == 4:
            return Long(take(">q"))
        if tag == 5:
            return Float(take(">f"))
        if tag == 6:
            return take(">d")
        if tag == 7:
            return [Byte(take(">b")) for _ in range(take(">i"))]
        if tag == 8:
            return string()
        if tag == 9:
            kind = take(">b")
            return [payload(kind) for _ in range(take(">i"))]
        if tag == 10:
            result = {}
            while True:
                kind = take(">b")
                if kind == 0:
                    return result
                key = string()
                result[key] = payload(kind)
        if tag == 11:
            return [take(">i") for _ in range(take(">i"))]
        if tag == 12:
            return [Long(take(">q")) for _ in range(take(">i"))]
        raise ValueError(tag)

    assert take(">b") == 10
    string()
    return payload(10)
