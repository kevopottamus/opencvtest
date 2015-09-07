#include <jni.h>

#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <vector>
#include <dirent.h>

#include <iostream>
#include <fstream>
#include <sstream>

#include <android/log.h>

#define LOG_TAG "detectPeople"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define EIGEN_FACE 1
#define FISHER_FACE 2
#define LBPH_FACE 3
#define TARGET_WIDTH 100
#define TARGET_HEIGHT 120
// training face expansion factor
#define EXPANSION_FACTOR 1.1f

using namespace std;
using namespace cv;

HOGDescriptor hog;
CascadeClassifier faceDetector;
Ptr<FaceRecognizer> recognizer;
vector<Mat> trainingImages;
vector<int> trainingLabels;
map<int, string> names;
int recognizerImageWidth, recognizerImageHeight;
bool requiresResize=-1;
VideoWriter videoWriter;
bool videoWriterReleased=0;

void initializeHog() {
	LOGD("initialize hog");
	hog.setSVMDetector(HOGDescriptor::getDefaultPeopleDetector());
}

void drawRectangle(Mat frame) {
    rectangle(frame, Point(10,10), Point(50,50), Scalar(255,0,0), 3);
}

void detectPeople(Mat frame, bool isFlip) {
    vector<Rect> found, found_filtered;

    // we shouldn't need to flip anything - if we always use landscape mode
    if (isFlip) {
		Mat flippedFrame;
		flip(frame, flippedFrame, 1);
		flippedFrame.copyTo(frame);
    }

    hog.detectMultiScale(frame, found, 0, Size(8,8), Size(32,32), 1.05, 2);

    LOGD("found %d", found.size());

    for (int i = 0; i < found.size(); ++i) {
        Rect r = found[i];
        int j = 0;
        for (; j < found.size(); ++j) {
        	// what does & mean for Rect?
            if (j != i && (r & found[j]) == r) {
                break;
            }
        }
        if (j == found.size()) {
            found_filtered.push_back(r);
        }
    }

    for (int i = 0; i < found_filtered.size(); ++i) {
        Rect r = found_filtered[i];
        rectangle(frame, r.tl(), r.br(), Scalar(255,0,0), 3);
    }
}

void initializeFaceDetector(const char* haarPath) {
    faceDetector.load(haarPath);
}

/* read training data in directory. initialize training image width and height */
void readDirectory(const string& filename, vector<Mat>& images, vector<int>& labels) {
    char imageDirName[1024];
    char imageName[1024];
    DIR* dirp = opendir(filename.c_str());
    if (dirp != NULL) {
        dirent* dp;
        while ((dp = readdir(dirp)) != NULL) {
            // only check directory and ignore . and ..
            if (dp->d_type == DT_DIR && strcmp(dp->d_name, ".") && strcmp(dp->d_name, "..")) {
                LOGD("%s", dp->d_name);

                strcpy(imageDirName, filename.c_str());
                strcpy(imageDirName+filename.length(), "/");
                strcpy(imageDirName+filename.length()+1, dp->d_name);

                DIR* imageDir = opendir(imageDirName);
                if (imageDir != NULL) {
                    dirent* imageFile;
                    while ((imageFile = readdir(imageDir)) != NULL) {
                        if (imageFile->d_type == DT_REG && imageFile->d_name[0] != '.') {
                            strcpy(imageName, imageDirName);
                            strcpy(imageName + strlen(imageDirName), "/");
                            strcpy(imageName + strlen(imageDirName) + 1, imageFile->d_name);

                            Mat m = imread(string(imageName), IMREAD_GRAYSCALE);
                            LOGD("image %s %d %d", imageName, m.rows, m.cols);

                            if (recognizerImageWidth<=0) {
                            	recognizerImageWidth=m.cols;
                            	recognizerImageHeight=m.rows;
                            }

                            images.push_back(m);
                            labels.push_back(atoi(dp->d_name));
                        }
                    }
                }
                closedir(imageDir);
            }
        }
    }
    closedir(dirp);
}

/* read name file which holds index to name map */
static void readNames(const string& namedir, map<int, string>& names, char separator = '=') {
    char mapName[1024];

    strcpy(mapName, namedir.c_str());
    strcpy(mapName+namedir.length(), "/");
    strcpy(mapName+namedir.length()+1, "name.txt");

    std::ifstream file(mapName, ifstream::in);
    if (!file) {
    	LOGD("%s", "Cannot find name map");
    	return;
    }
    names.clear();
    string line, indexString, nameString;
    while (getline(file, line)) {
        stringstream liness(line);
        getline(liness, indexString, separator);
        getline(liness, nameString);
        if(!indexString.empty() && !nameString.empty()) {
            names[atoi(indexString.c_str())]=nameString;
        }
    }
}

void trainRecognizer(const char* rootPath, int type) {
	readDirectory(string(rootPath), trainingImages, trainingLabels);
	readNames(string(rootPath), names);

	if (trainingImages.size() > 0) {
		if (type==EIGEN_FACE) {
			recognizer = createEigenFaceRecognizer();
			requiresResize=-1;
		} else if (type==FISHER_FACE) {
			recognizer = createFisherFaceRecognizer();
			requiresResize=-1;
		} else {
			recognizer = createLBPHFaceRecognizer();
			requiresResize=0;
		}
		recognizer->train(trainingImages, trainingLabels);
	}
}

// expand rect, if possible.  otherwise leave rect as is
Rect expand(Rect rect, float factor, int maxWidth, int maxHeight) {
    int newWidth = rect.width * factor;
    int newHeight = rect.height * factor;

    int marginX = (newWidth - rect.width)/2;
    int marginY = (newHeight - rect.height)/2;
    int newX = rect.x - marginX;
    int newY = rect.y - marginY;

    if (newX>=0 && newY>=0 && (newX+newWidth)<=maxWidth && (newY+newHeight)<=maxHeight) {
        return Rect(newX, newY, newWidth, newHeight);
    } else {
        return rect;
    }
}

/* get face rectangle that matches target ratio */
Rect getTargetInRatio(Rect face, int imageWidth, int imageHeight, float targetRatio) {
    LOGD("image %d %d\n", imageWidth, imageHeight);
    LOGD("face rect=%d %d %d %d\n", face.x, face.y, face.width, face.height);
    face = expand(face, EXPANSION_FACTOR, imageWidth, imageHeight);
    LOGD("expanded face rect=%d %d %d %d\n", face.x, face.y, face.width, face.height);
    float ratio=(float)face.width/face.height;

    Rect result;
    int newWidth = face.width;
    int newHeight = face.height;
    int newX = face.x;
    int newY = face.y;
    if (ratio < targetRatio) {    // need to increase width
        newWidth = face.height*targetRatio;
        if (newWidth < imageWidth) {    // new width fits, update x if needed
            newX = face.x - (newWidth-face.width)/2;
            if (newX < 0) {
                newX = 0;
            }
            if (newX+newWidth > imageWidth) {
                newX=imageWidth - newWidth;
            }
        } else { // fix width, shrink height instead
            newWidth = imageWidth;
            newHeight = newWidth/targetRatio;
            newX = 0;
            newY = face.y - (newHeight-face.height)/2;
            // no need for boundary check on newY as we are shrinking height
        }
    } else {    // need to increase height
        newHeight=face.width/targetRatio;
        if (newHeight < imageHeight) {    // new height fits, update y if necessary
            newY = face.y - (newHeight-face.height)/2;
            if (newY<0) {
                newY=0;
            }
            if (newY+newHeight > imageHeight) {
                newY=imageHeight - newHeight;
            }
        } else { // fix height, shrink width
            newWidth = newHeight*targetRatio;
            newHeight = imageHeight;
            newY = 0;
            newX = face.x - (newWidth-face.width)/2;
            // no need for boundary check on newX as we are shrinking width
        }
    }
    result = Rect(newX, newY, newWidth, newHeight);
    LOGD("result rect=%d %d %d %d\n", result.x, result.y, result.width, result.height);
    return result;
}

int addTrainingData(Mat image, const char* name) {
    Mat gray;
    cvtColor(image, gray, COLOR_BGR2GRAY);

    vector< Rect_<int> > faces;
    faceDetector.detectMultiScale(gray, faces);

    LOGD("found %d faces", faces.size());
    // TODO what do we do if there is more than 1 face in training data

    if (faces.size() > 0) {
        Rect faceRect = faces[0];
        float ratio;
        if (recognizerImageWidth <= 0) {
        	ratio = (float) TARGET_WIDTH / TARGET_HEIGHT;
        } else {
        	ratio = (float) recognizerImageWidth / recognizerImageHeight;
        }
        Rect faceInRatio = getTargetInRatio(faceRect, gray.cols, gray.rows, ratio);

        Mat face = gray(faceInRatio);
        Mat faceResized;
        cv::resize(face, faceResized, Size(recognizerImageWidth, recognizerImageHeight), 1.0, 1.0, INTER_CUBIC);

        imwrite(string(name), faceResized);
    }

    return faces.size();
}

void showRecognized(Mat frame, Rect faceRect, string prediction) {
	// draw rectangle
    rectangle(frame, faceRect, Scalar(0, 255,0), 1);

    // And now put prediction into the image:
    int posX = std::max(faceRect.tl().x - 10, 0);
    int posY = std::max(faceRect.tl().y - 10, 0);
    putText(frame, prediction, Point(posX, posY), FONT_HERSHEY_PLAIN, 1.0, Scalar(0, 255, 0), 2.0);
}

void showVideo(Mat frame) {
	Mat gray;
	cvtColor(frame, gray, COLOR_BGR2GRAY);

}

void recognize(Mat frame) {
	Mat gray;
	vector< Rect_<int> > faces;

	cvtColor(frame, gray, COLOR_BGR2GRAY);
	faceDetector.detectMultiScale(gray, faces);
	LOGD("found %d faces", faces.size());

	for (int i=0; i<faces.size(); i++) {
		Rect faceRect = faces[i];
		Mat faceResized;
		if (requiresResize) {
			float ratio = (float) recognizerImageWidth / recognizerImageHeight;
			Rect faceInRatio = getTargetInRatio(faceRect, gray.cols, gray.rows, ratio);
			Mat face = gray(faceRect);
			cv::resize(face, faceResized, Size(recognizerImageWidth, recognizerImageHeight), 1.0, 1.0, INTER_CUBIC);
		} else {
			faceResized = gray(faceRect);
		}

		int prediction = recognizer->predict(faceResized);
		map<int, string>::iterator iterator = names.find(prediction);

		if (iterator == names.end()) {	// key not found
			LOGD("face %d: %d %d %d %d unrecognized", i, faceRect.x, faceRect.y, faceRect.width, faceRect.height);
		} else {
			string name = iterator->second;
			LOGD("face %d: %d %d %d %d %s", i, faceRect.x, faceRect.y, faceRect.width, faceRect.height, name.c_str());
			showRecognized(frame, faceRect, name);
		}
	}

}

void initializeVideoWriter(const char* fileName, int width, int height, int fps) {
	Size size = Size(width, height);
	//int fourcc = VideoWriter::fourcc('F','L','V','1');
	int fourcc = CV_FOURCC('F', 'L', 'V', '1');
	bool opened = videoWriter.open(fileName, fourcc, fps, size, true);
	printf("video opened=%d %dx%d %d fps\n", opened, width, height, fps);
	videoWriterReleased=0;
}

void writeVideo(Mat frame) {
    videoWriter.write(frame);
}

extern "C" {
	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializePeopleDetector(JNIEnv*, jobject);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeFaceDetector(JNIEnv*, jobject, jstring);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeRecognizer(JNIEnv*, jobject, jstring, jint);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeVideoWriter(JNIEnv*, jobject, jstring, jint, jint);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_findPeople(JNIEnv*, jobject, jlong);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeInternalTraining(JNIEnv*, jobject, jstring);

	JNIEXPORT jint JNICALL Java_com_test_opencvtest_OpenCvUtility_addTrainingData(JNIEnv*, jobject, jlong, jstring);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_findPeopleInFile(JNIEnv*, jobject, jstring, jstring);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_recognizePeople(JNIEnv*, jobject, jlong);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_showVideo(JNIEnv*, jobject, jlong);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_writeVideo(JNIEnv*, jobject, jlong);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_releaseVideo(JNIEnv*, jobject);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_recognizePeopleInFile(JNIEnv*, jobject, jstring, jstring);

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializePeopleDetector(JNIEnv*, jobject) {
		initializeHog();
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeFaceDetector(JNIEnv* jEnv, jobject jObj, jstring jHaarPath) {
		const char* haarPath = jEnv->GetStringUTFChars(jHaarPath, NULL);
		initializeFaceDetector(haarPath);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeInternalTraining(JNIEnv* jEnv, jobject jObj, jstring jTrainingPath) {
		const char* trainingPath = jEnv->GetStringUTFChars(jTrainingPath, NULL);
		readDirectory(string(trainingPath), trainingImages, trainingLabels);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeRecognizer(JNIEnv* jEnv, jobject jObj, jstring jTrainingPath, jint jType) {
		const char* trainingPath = jEnv->GetStringUTFChars(jTrainingPath, NULL);
		trainRecognizer(trainingPath, jType);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_nativeInitializeVideoWriter(JNIEnv* jEnv, jobject jObj, jstring jPath, jint width, jint height) {
		const char* path = jEnv->GetStringUTFChars(jPath, NULL);
		initializeVideoWriter(path, width, height, 10);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_findPeople(JNIEnv* jEnv, jobject jObj, jlong addrFrame) {
		Mat& mFrame  = *(Mat*)addrFrame;
		detectPeople(mFrame, 0);
		//drawRectangle(mFrame);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_findPeopleInFile(JNIEnv* jEnv, jobject jObj, jstring jInputName, jstring jOutputName) {
		const char* inputName = jEnv->GetStringUTFChars(jInputName, NULL);
		const char* outputName = jEnv->GetStringUTFChars(jOutputName, NULL);

		Mat image=imread(inputName, IMREAD_UNCHANGED);
		detectPeople(image, 0);
		imwrite(outputName, image);
	}

	JNIEXPORT jint JNICALL Java_com_test_opencvtest_OpenCvUtility_addTrainingData(JNIEnv* jEnv, jobject jObj, jlong jFrame, jstring jOutputName) {
		Mat& mFrame  = *(Mat*)jFrame;
		const char* outputName = jEnv->GetStringUTFChars(jOutputName, NULL);

		return addTrainingData(mFrame, outputName);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_recognizePeople(JNIEnv* jEnv, jobject jObj, jlong addrFrame) {
		Mat& mFrame  = *(Mat*)addrFrame;
		recognize(mFrame);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_showVideo(JNIEnv* jEnv, jobject jObj, jlong addrFrame) {
		Mat& mFrame  = *(Mat*)addrFrame;
		showVideo(mFrame);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_releaseVideo(JNIEnv* jEnv, jobject jObj) {
		if (!videoWriterReleased) {
			videoWriter.release();
			videoWriterReleased=-1;
		}
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_writeVideo(JNIEnv* jEnv, jobject jObj, jlong addrFrame) {
		Mat& mFrame  = *(Mat*)addrFrame;
		writeVideo(mFrame);
	}

	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_recognizePeopleInFile(JNIEnv* jEnv, jobject jObj, jstring jInputName, jstring jOutputName) {
		const char* inputName = jEnv->GetStringUTFChars(jInputName, NULL);
		const char* outputName = jEnv->GetStringUTFChars(jOutputName, NULL);

		Mat image=imread(inputName, IMREAD_UNCHANGED);
		recognize(image);
		imwrite(outputName, image);
	}

	/* test code */
	JNIEXPORT void JNICALL Java_com_test_opencvtest_OpenCvUtility_test(JNIEnv* jEnv, jobject jObj, jstring jHaarName, jstring jImageName) {
		const char* haarName = jEnv->GetStringUTFChars(jHaarName, NULL);
		const char* imageName = jEnv->GetStringUTFChars(jImageName, NULL);

	    //CascadeClassifier faceDetector;
	    //faceDetector.load(string(haarName));

	    Mat testImage = imread(string(imageName), IMREAD_UNCHANGED);
	    vector< Rect_<int> > faces;
	    faceDetector.detectMultiScale(testImage, faces);

	    int count = (int) faces.size();
	    LOGD("faces = %d", count);
	}
}
