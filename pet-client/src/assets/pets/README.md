# 宠物形象素材目录

M4 拍板:使用开源素材 + 帧动画管线;沙箱无法联网下载素材,当前先用 **emoji 占位** 运行,
把素材按下面目录结构放好后,前端会自动优先使用图片。

## 目录约定

```
src/assets/pets/{typeCode}/{gender}/{action}/{n}.png
```

- `typeCode`: RAT / OX / TIGER / RABBIT / DRAGON / SNAKE / HORSE / GOAT / MONKEY / ROOSTER / DOG / PIG / CAT
- `gender`: MALE / FEMALE
- `action`: idle(待机) / play(玩耍) / happy(开心) / danger(病危) / dead(死亡)
- `n.png`: 帧序号,从 1 开始,按顺序轮播即帧动画

示例:`src/assets/pets/CAT/MALE/idle/1.png`、`2.png`、`3.png`…

## 素材来源建议(免费)

- itch.io 搜索 `pixel pet sprite` / `animal sprite sheet`(注意确认授权允许商用/个人使用)
- OpenGameArt(CC0 / CC-BY 素材)

## 注意事项

- 图片建议统一尺寸(如 64×64 或 96×96),透明背景 PNG。
- 缺某一帧时自动回退到 emoji + CSS 动画,不会报错。
