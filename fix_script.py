import re

filepath = "Bcore/src/main/java/top/niunaijun/blackbox/fake/service/IActivityManagerProxy.java"

with open(filepath, "r") as f:
    content = f.read()

content = content.replace("#<<<<<<< codex/fix-array-null-pointer-exceptions-6gx866", "")
content = content.replace("#=======", "")
content = content.replace("#>>>>>>> main", "")

with open(filepath, "w") as f:
    f.write(content)
