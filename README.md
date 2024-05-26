## Load Balancing

Master와 Server의 이기종 컴퓨팅 자원 공유를 통해 향상된 fps로 ADAS 구현

- Master : [블랙박스](http://itempage3.auction.co.kr/DetailView.aspx?ItemNo=C522794645&frm3=V2)
- Server : 갤럭시 시리즈(S22, S7, S8)

|     | fps |  Homogeneous computing   |
|:---:|:---:|:---:|
| FCW | 9~10|  ![](https://github.com/bert13069598/LoadBalancing/assets/89738612/a625a39a-fed9-448d-848c-0c1bef885f7d)   |
| LDW | 1~2 |  ![](https://github.com/bert13069598/LoadBalancing/assets/89738612/68fa1092-5d22-4caa-9233-429bce4cdf3d)   |

|           | fps | Heterogeneous computing    |
|:---------:|:---:|:---:|
| FCW<br>&<br>LDW |  20~   |  ![](https://github.com/bert13069598/LoadBalancing/assets/89738612/5ad99c34-876c-4e94-8271-2106833160c5)   |

### Setting

- Gradle JDK jbr-17
- CMake 3.22.1
- [opencv-mobile-4.8.0-android.zip](https://github.com/nihui/opencv-mobile/releases/download/v18/opencv-mobile-4.8.0-android.zip)
- [ncnn-20230816-android.zip](https://github.com/Tencent/ncnn/releases/download/20230816/ncnn-20230816-android.zip)

### IP setting

연결된 IP가 오름차순으로 정렬됨  
순서에 맞게 각 휴대폰에 IP PORT_NUMBER를 설정

|       Master       |        Server        |
|:------------------:|:--------------------:|
| java > Sender.java | java > Receiver.java |
| clientSocket port  |     PORT_NUMBER      |

### reference

https://github.com/RangiLyu/nanodet  
https://github.com/ultralytics/ultralytics  
https://github.com/nihui/ncnn-android-nanodet  
https://github.com/FeiGeChuanShu/yolov5-seg-ncnn  
