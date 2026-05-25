# SaCameraMod

[![Forge Version](https://img.shields.io/badge/Forge-1.16.5--36.2.42-blue)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**Advanced camera control mod for Minecraft 1.16.5 Forge.**

SaCameraMod adds cinematic camera controls with shake, smooth movement, rotation, path interpolation, and perspective switching through `/sacamera` commands.

## Features

- **Camera Shake** — positional or rotational shake with adjustable strength and duration.
- **Smooth Movement** — world-axis movement, yaw-relative movement, and forward-camera directional movement with fade-in, hold, and fade-out transitions.
- **Rotation** — rotate around X, Y, or Z axis, including roll on the Z axis.
- **Cinematic Keyframes** — define multi-keyframe camera paths with interpolation and easing (`linear`, `easein`, `easeout`, `easeinout`).
- **Perspective Toggle** — force first-person, third-person back, or third-person front view.
- **Cancel** — stop all active effects and restore the default camera.

## Commands

Base syntax:

```text
/sacamera <target> <subcommand> [args...]
```

`<target>` can be a player selector like `@s`, `@a`, or a player name.

| Subcommand | Description |
|---|---|
| `shake positioned <strength> <duration>` | Positional shake |
| `shake rotationed <strength> <duration>` | Rotational shake |
| `move <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | Move in world coordinates |
| `move relative <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | Move relative to player yaw |
| `move forward <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | Move along camera forward direction |
| `rotate <axis> <angle> <fadeIn> <hold> <fadeOut>` | Absolute rotation (axis = x/y/z) |
| `rotate relative <axis> <angle> <fadeIn> <hold> <fadeOut>` | Relative rotation |
| `cinema <duration> <keyframeList>` | Play cinematic keyframe path |
| `toggleperspective <mode>` | Switch camera mode: `0`=first-person, `1`=third-person back, `2`=third-person front |
| `cancel` | Cancel all active camera effects |

## Cinema Keyframe Syntax

Use semicolons (`;`) to separate keyframes.

- Simple keyframe: `x,y,z,yaw,pitch[,ease]`
- World absolute keyframe: `world:x,y,z,targetX,targetY,targetZ[,ease]`

Example easing options:

- `linear` — no easing
- `easein`
- `easeout`
- `easeinout`

## Examples

```text
/sacamera @s shake positioned 0.5 20
/sacamera @s move forward 0 0 1 0 40 10
/sacamera @s rotate relative y 45 10 40 10
/sacamera @s cinema 100 1,4,1,45,-10;5,5,5,50,-5
/sacamera @s cinema 100 world:1,5,1,0,4,0,easein;world:10,8,10,0,4,0,easeinout
/sacamera @s toggleperspective 1
/sacamera @s cancel
```

## Notes

- Commands require operator permission (OP) on multiplayer servers.
- The camera control is sent from the server to each target client.
- In integrated single-player, the server still sends control packets to the client.
- The mod does not force-load unloaded chunks. Camera effects entering unloaded chunks may not render correctly.

## 中文说明

### 功能

- **摄像机抖动** — 支持位置抖动和角度抖动，可调节强度和持续时间。
- **平滑移动** — 支持世界坐标移动、相对玩家朝向移动、以及沿摄像机前方方向移动，提供淡入、保持、淡出过渡。
- **旋转** — 支持绕 X、Y、Z 轴旋转，Z 轴代表横滚（roll）。
- **关键帧运镜** — 支持多个关键帧路径，自动插值并支持缓动（`linear`、`easein`、`easeout`、`easeinout`）。
- **视角切换** — 强制切换第一人称、第三人称背后、第三人称正面视角。
- **取消** — 停止当前所有效果并恢复默认摄像机视角。

### 指令格式

```text
/sacamera <target> <subcommand> [args...]
```

`<target>` 可使用选择器 `@s`、`@a`，或直接填写玩家名称。

| 子命令 | 说明 |
|---|---|
| `shake positioned <strength> <duration>` | 位置抖动 |
| `shake rotationed <strength> <duration>` | 角度抖动 |
| `move <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | 世界坐标移动 |
| `move relative <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | 相对玩家朝向移动 |
| `move forward <dx> <dy> <dz> <fadeIn> <hold> <fadeOut>` | 沿摄像机前方向移动 |
| `rotate <axis> <angle> <fadeIn> <hold> <fadeOut>` | 绝对旋转（轴 x/y/z） |
| `rotate relative <axis> <angle> <fadeIn> <hold> <fadeOut>` | 相对旋转 |
| `cinema <duration> <keyframeList>` | 播放关键帧运镜路径 |
| `toggleperspective <mode>` | 切换视角：`0`=第一人称，`1`=第三人称背后，`2`=第三人称正面 |
| `cancel` | 取消所有摄像机效果 |

### 关键帧语法

使用分号 `;` 分隔关键帧。

- 简单关键帧：`x,y,z,yaw,pitch[,ease]`
- 世界绝对关键帧：`world:x,y,z,targetX,targetY,targetZ[,ease]`

缓动选项：

- `linear` — 线性
- `easein`
- `easeout`
- `easeinout`

### 示例

```text
/sacamera @s shake positioned 0.5 20
/sacamera @s move forward 0 0 1 0 40 10
/sacamera @s rotate relative y 45 10 40 10
/sacamera @s cinema 100 1,4,1,45,-10;5,5,5,50,-5
/sacamera @s cinema 100 world:1,5,1,0,4,0,easein;world:10,8,10,0,4,0,easeinout
/sacamera @s toggleperspective 1
/sacamera @s cancel
```

### 注意事项

- 多人服务器中执行指令需要 OP 权限。
- 指令会由服务器发送控制包到目标客户端。
- 在单人模式中，服务端仍会向客户端发送控制包。
- 本 mod 不会强制加载未生成区块，进入未加载区块时画面可能无法正确渲染。

## Installation

1. Place the built mod JAR into your Minecraft `mods` folder.
2. Run Forge 1.16.5 with the corresponding Forge version.
3. Use `/sacamera` commands in-game with OP permission.

## License

This mod is released under the MIT License. See `LICENSE` for details.

## Author

`shiny_Asuna` (`shinyAsuna001`)

## AI Disclosure

Part of the code in this project was generated with the assistance of AI tools. Final implementation and design decisions were reviewed and integrated by the human author.
