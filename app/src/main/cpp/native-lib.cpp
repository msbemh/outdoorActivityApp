#include <jni.h>

#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <iostream>
//#include <dlib/image_processing/frontal_face_detector.h>
//#include <dlib/image_processing/render_face_detections.h>
//#include <dlib/image_processing.h>
//#include <dlib/image_transforms.h>
//#include <dlib/image_io.h>
//#include <dlib/opencv/cv_image.h>

using namespace cv;
using namespace std;
//using namespace dlib;

void overlayImage(const Mat &background, const Mat &foreground,
                  Mat &output, Point2i location);

// cv::Rect dlibRectangleToOpenCV(dlib::rectangle r);

extern "C"
JNIEXPORT void JNICALL
Java_com_action_outdooractivityapp_activity_FaceDetectCameraActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                      jlong mat_addr_input, jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_action_outdooractivityapp_activity_FaceDetectCameraActivity_Detect(JNIEnv *env, jobject instance,
                                                jlong mat_addr_input, jlong mat_addr_result) {
    //안드로이드에 로그표시
    __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ", "start");
    try {

        Mat &matInput = *(Mat *)mat_addr_input;
        Mat &matResult = *(Mat *)mat_addr_result;

        cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
    } catch (Exception e){
        //안드로이드에 로그표시
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ", "exception thrown! %s",  e.what() );
    }
}

//cv::Rect dlibRectangleToOpenCV(dlib::rectangle r){
//    return cv::Rect(cv::Point2i(r.left(), r.top()), cv::Point2i(r.right() + 1, r.bottom() + 1));
//}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_action_outdooractivityapp_activity_FaceDetectCameraActivity_loadCascade(JNIEnv *env, jobject thiz,
                                                     jstring cascade_file_name) {
    const char *nativeFileNameString = env->GetStringUTFChars(cascade_file_name, 0);

    //String객체로 경로생성
    string baseDir("/storage/emulated/0/");
    baseDir.append(nativeFileNameString);
    //String => char* 형태의 문자열로 변경
    const char *pathDir = baseDir.c_str();

    jlong ret = 0;
    ret = (jlong) new CascadeClassifier(pathDir);
    if (((CascadeClassifier *) ret)->empty()) {
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
    }else
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);
    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);

    return ret;
}

float resizeImg(Mat img_src, Mat &img_resize, int resize_width){
    //리사이즈 비율 계산
    float scale = resize_width / (float)img_src.cols ;

    //작게 리사이즈 할떄
    if (img_src.cols > resize_width) {
        //반올림
        int new_height = cvRound(img_src.rows * scale);
        resize(img_src, img_resize, Size(resize_width, new_height));
    //크게 리사이즈 할때
    }else {
        img_resize = img_src;
    }
    return scale;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_action_outdooractivityapp_activity_FaceDetectCameraActivity_detect(JNIEnv *env, jobject thiz,
                                                jlong cascade_classifier_face,
                                                jlong cascade_classifier_eye,
                                                jlong mat_addr_input,
                                                jlong mat_addr_result,
                                                jint mask_flag) {
    Mat &img_input = *(Mat *) mat_addr_input;
    Mat &img_result = *(Mat *) mat_addr_result;
    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "마스크 플래그값:%d",mask_flag);
    //가면 영상 가져오기
    Mat glasses = imread("/storage/emulated/0/sunglasses.png", IMREAD_COLOR);
    if (!glasses.data) {
        __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "선글라스 사진 인식못함");
    }
    //BRG을 RGB로 변형
    cvtColor(glasses, glasses, COLOR_BGR2RGB);

    img_result = img_input.clone();

    std::vector<Rect> faces;
    Mat img_gray;

    //컬러=>흑백 변환
    cvtColor(img_input, img_gray, COLOR_BGR2GRAY);
    //한쪽으로 치우쳐진 화질을 평활화하여 보여주기.
    equalizeHist(img_gray, img_gray);

    //이미지 리사이즈함. 결과는 img_resize에 들어감. 리사이즈 비율반환해줌.
    Mat img_resize;
    float resizeRatio = resizeImg(img_gray, img_resize, 640);

    //-- Detect faces
    //cascase로 얼굴을 인식하여 faces에 Rect들을 저장
    ((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(20, 20) );
    //로그 찍기
    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "face %d found ", faces.size());
    //얼굴인식한 개수만큼 반복
    for (int i = 0; i < faces.size(); i++) {
        //원본이미지에 맞게 사각형 사이즈변형.
        double real_facesize_x = faces[i].x / resizeRatio;
        double real_facesize_y = faces[i].y / resizeRatio;
        double real_facesize_width = faces[i].width / resizeRatio;
        double real_facesize_height = faces[i].height / resizeRatio;

        //사각형의 중심점
        Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);

        //사각형 생성
        Rect face_area(real_facesize_x, real_facesize_y, real_facesize_width,real_facesize_height);
        //img_gray에서 관심영역 추출
        //ROI란 Region Of Interest(관심영역)
        Mat faceROI = img_gray( face_area );

        //눈을 표시할 사각형 리스트 선언
        std::vector<Rect> eyes;

        //관심영역에서 눈 검출
        ((CascadeClassifier *) cascade_classifier_eye)->detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(20, 20) );

        //선글라스 마스크일 경우
        if(mask_flag == 1){
            //눈인식을 2개로 알맞게 한 경우
            if(eyes.size() == 2){
                Point center1( real_facesize_x + eyes[0].x + eyes[0].width/2, real_facesize_y + eyes[0].y + eyes[0].height/2 );
                Point center2( real_facesize_x + eyes[1].x + eyes[1].width/2, real_facesize_y + eyes[1].y + eyes[1].height/2 );

                //center1을 왼쪽눈으로 잡기
                if ( center1.x > center2.x ){
                    Point temp;
                    temp = center1;
                    center1 = center2;
                    center2 = temp;
                }

                //2개의 눈 사이의 너비와 높이 계산
                int width = abs(center2.x - center1.x);
                int height = abs(center2.y - center1.y);

                //눈은 눈사이의 너비가 높이보다 커야함.
                if ( width > height){
                    //330은 이미지 선글라스의 두눈 사이의 거리
                    float imgScale = width/330.0;

                    //선글라스 너비와 높이 다시 조정
                    int w, h;
                    w = glasses.cols * imgScale;
                    h = glasses.rows * imgScale;

                    //오프셋으로 선글라스 위치 조정
                    //선글라스의 왼쪽눈 기준 위치가 (150,160)이기 때문에
                    //선글라스 시작위치를 눈의 중심에서 offset만큼 빼준다.
                    int offsetX = 150 * imgScale;
                    int offsetY = 160 * imgScale;

                    //선글라스 사이즈 조정
                    Mat resized_glasses;
                    resize( glasses, resized_glasses, cv::Size( w, h), 0, 0 );

                    //첫번째 인자: Mat &background
                    //두번째 인자: Mat &foreground
                    //세번째 인자: Mat &output
                    //네번째 인자: Point 시작위치
                    overlayImage(img_result, resized_glasses, img_result, Point(center1.x-offsetX, center1.y-offsetY));
                }
                //인식한 눈의 객수가 2개가 아닐 경우
            }else{
                //타원 그리기
                //img_result를 변화시키면 Android에서의 이미지도 변화됨.
                //Scalar로 색표현
                // x축 방향 반지름 길이 200, y축 방향 반지름 길이 10인 파란색 타원을 그립니다.
                //img,        타원이 그려질 이미지
                //center,        중심 좌표(x, y)
                //axes,        메인 축 방향의 반지름
                //angle,        회전각
                //startAngle,        호의 시작각도
                //endAngle,        호의 끝각도
                //color,        타원의 색( B, G, R )
                //굵기
                //라인타입
                //shift

//                ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360,
//                        Scalar(255, 0, 255), 4, 8, 0);
//
//                //검출한 눈 갯수만큼 반복
//                for ( size_t j = 0; j < eyes.size(); j++ ){
//                    //원본이미지에서의 눈 중심점 계산
//                    Point eye_center( real_facesize_x + eyes[j].x + eyes[j].width/2, real_facesize_y + eyes[j].y + eyes[j].height/2 );
//                    //반지름 계산
//                    int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );
//                    //원 img_result에 넣기
//                    circle( img_result, eye_center, radius, Scalar( 255, 0, 0 ), 4, 8, 0 );
//                }
            }
        }
    }
}

//이미지 합치기
void overlayImage(const Mat &background, const Mat &foreground,
                 Mat &output, Point2i location){
    //background를 output으로 깊은복사
    background.copyTo(output);

    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "foreground.rows:%d", foreground.rows);
    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "foreground.cols:%d",foreground.cols);
    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "background.rows:%d", background.rows);
    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ", (char *) "background.cols:%d",background.cols);

    //location의 위치는 합성하고자 하는 사진의 시작점(왼쪽,위쪽)
    //location의 시작점부터 시작함.
    //max(a,b)는 a와b 중 큰값 반환 => 시작위치(y)가 음수일때 0을 시작위치(y)로 정해줌.
    for (int y = std::max(location.y, 0); y < background.rows; y++){
        //y축 평행이동 이동거리
        int fY = y - location.y;

        //합성하려는 이미지의 y크기를 초과한경우 for문 중지
        if (fY >= foreground.rows){
            break;
        }


        //location 시작점(x)부터 시작.
        //max(a,b)는 a와b 중 큰값 반환 => 시작위치(x)가 음수일때 0을 시작위치(x)로 정해줌.
        for (int x = std::max(location.x, 0); x < background.cols; x++){
            //x축 평행이동 이동거리
            int fX = x - location.x;

            //합성하려는 이미지의 x크기를 초과한경우 for문 중지
            if (fX >= foreground.cols){
                break;
            }


            //합성하려는 이미지의 alpha채널(광도) 가져오기.
            //Mat.data는 행렬데이터의 포인터
            //Mat.data를 이용하여 Mat.data[]로 Mat의 원하는 위치의 데이터를 가져올 수 있다.
            //Mat.step은 행의 바이트수를 가져온다.
            //data[WANT_ROW *  image.cols + WANT_COL]
            //- ROW : 행
            //- COL : 열
            //- CV_TYPE : 데이터 타입(예: CV_8UC3 = 8 bit 3 channels)
            //- DATA_TYPE : Mat 생성시 데이터 타입(예: float, usigned char)
            //- WANT_ROW : 접근하기 원하는 행
            //- WANT_COL : 접근하기 원하는 열
            double opacity =
                    ((double)foreground.data[fY * foreground.step + fX * foreground.channels() + 3]) / 255;
            // and now combine the background and foreground pixel, using the opacity,
            // but only if opacity > 0.
            //현재 가리키는 x,y위치의 채널갯수만큼 반복하여 output의 data[]값 정하기.
            //현재 가리키는 x,y안속으로 들어가면 채널이 1일경우 0~255
            //                                      3일 경우 0~255, 0~255, 0~255 이렇게 데이터가 구성돼있다.
            //Mat.data로 각 RGB에 해당하는 채널데이터까지 다룰 수 있다.
            for (int c = 0; opacity > 0 && c < output.channels(); c++) {
                //실질적 합성이미지의 위치(채널포함하여)
                unsigned char foregroundPx =
                        foreground.data[fY * foreground.step + fX * foreground.channels() + c];
                //실질적 배경이미지의 위치(채널포함하여)
                unsigned char backgroundPx =
                        background.data[y * background.step + x * background.channels() + c];
                //결과이미지 하나하나에 데이터 입력
                //opacity투명도
                //1 : 완전 불투명
                //0 : 완전 투명
                //합성이미지가 완전투명하면 합성이미지의 값이 들어가고
                //합성이미지가 완전투명하면 배경이미지가 들어간다.
                //이러한 비율로 둘이 섞어서 데이터가 들어가진다.
                output.data[y*output.step + output.channels()*x + c] =
                        backgroundPx * (1. - opacity) + foregroundPx * opacity;
            }
        }
    }
}


//extern "C"
//JNIEXPORT void JNICALL
//Java_com_tistory_webnautes_useopencvwithcmake_MainActivity_ConvertRGBtoGray(JNIEnv *env,
//                                                                            jobject instance,
//                                                                            jlong matAddrInput,
//                                                                            jlong matAddrResult) {
//
//    Mat &matInput = *(Mat *)matAddrInput;
//    Mat &matResult = *(Mat *)matAddrResult;
//
//    cvtColor(matInput, matResult, CV_RGBA2GRAY);
//
//}
