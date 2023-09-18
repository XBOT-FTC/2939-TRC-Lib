/*
 * Copyright (c) 2023 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package teamcode.vision;

import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.tfod.TfodProcessor;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import TrcCommonLib.trclib.TrcDbgTrace;
import TrcCommonLib.trclib.TrcOpenCvColorBlobPipeline;
import TrcCommonLib.trclib.TrcVisionTargetInfo;
import TrcFtcLib.ftclib.FtcEocvColorBlobProcessor;
import TrcFtcLib.ftclib.FtcOpMode;
import TrcFtcLib.ftclib.FtcVision;
import TrcFtcLib.ftclib.FtcVisionAprilTag;
import TrcFtcLib.ftclib.FtcVisionEocvColorBlob;
import TrcFtcLib.ftclib.FtcVisionTensorFlow;
import teamcode.Robot;
import teamcode.RobotParams;

/**
 * This class implements AprilTag/TensorFlow/Eocv Vision for the game season. It creates and initializes all the
 * vision target info as well as providing info for the robot, camera and the field. It also provides methods to get
 * the location of the robot and detected targets.
 */
public class Vision
{
    private static final String moduleName = "Vision";
    private static final int colorConversion = Imgproc.COLOR_BGRA2BGR;
    private static final double[] redBlobColorThresholds = {100.0, 255.0, 0.0, 100.0, 0.0, 60.0};
    private static final double[] blueBlobColorThresholds = {0.0, 60.0, 0.0, 100.0, 100, 255.0};
    private static final TrcOpenCvColorBlobPipeline.FilterContourParams colorBlobFilterContourParams =
        new TrcOpenCvColorBlobPipeline.FilterContourParams()
            .setMinArea(1000.0)
            .setMinPerimeter(100.0)
            .setWidthRange(10.0, 1000.0)
            .setHeightRange(10.0, 1000.0)
            .setSolidityRange(0.0, 100.0)
            .setVerticesRange(0.0, 1000.0)
            .setAspectRatioRange(0.0, 10.0);
    private static final String TFOD_MODEL_ASSET = "MyObject.tflite";
    private static final float TFOD_MIN_CONFIDENCE = 0.75f;
    public static final String[] TFOD_TARGET_LABELS = {"MyObject"};

    public FtcVisionAprilTag aprilTagVision;
    private AprilTagProcessor aprilTagProcessor;
    public FtcVisionEocvColorBlob redBlobVision;
    private FtcEocvColorBlobProcessor redBlobProcessor;
    public FtcVisionEocvColorBlob blueBlobVision;
    private FtcEocvColorBlobProcessor blueBlobProcessor;
    public FtcVisionTensorFlow tensorFlowVision;
    private TfodProcessor tensorFlowProcessor;
    private final FtcVision vision;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param robot specifies the robot object.
     * @param tracer specifies the tracer for trace info, null if none provided.
     */
    public Vision(Robot robot, TrcDbgTrace tracer)
    {
        FtcOpMode opMode = FtcOpMode.getInstance();
        ArrayList<VisionProcessor> visionProcessorList = new ArrayList<>();

        if (RobotParams.Preferences.useAprilTagVision)
        {
            robot.globalTracer.traceInfo(moduleName, "Starting AprilTagVision...");
            FtcVisionAprilTag.Parameters aprilTagParams = new FtcVisionAprilTag.Parameters()
                .setDrawTagIdEnabled(true)
                .setDrawTagOutlineEnabled(true)
                .setDrawAxesEnabled(false)
                .setDrawCubeProjectionEnabled(false)
                .setLensIntrinsics(
                    RobotParams.WEBCAM_FX, RobotParams.WEBCAM_FY, RobotParams.WEBCAM_CX, RobotParams.WEBCAM_CY)
                .setOutputUnits(DistanceUnit.INCH, AngleUnit.DEGREES);
            aprilTagVision = new FtcVisionAprilTag(aprilTagParams, AprilTagProcessor.TagFamily.TAG_36h11, tracer);
            aprilTagProcessor = aprilTagVision.getVisionProcessor();
            visionProcessorList.add(aprilTagProcessor);
        }

        if (RobotParams.Preferences.useColorBlobVision)
        {
            robot.globalTracer.traceInfo(moduleName, "Starting ColorBlobVision...");

            redBlobVision = new FtcVisionEocvColorBlob(
                "RedBlob", colorConversion, redBlobColorThresholds, colorBlobFilterContourParams,
                RobotParams.cameraRect, RobotParams.worldRect, true, tracer);
            redBlobProcessor = redBlobVision.getVisionProcessor();
            visionProcessorList.add(redBlobProcessor);

            blueBlobVision = new FtcVisionEocvColorBlob(
                "BlueBlob", colorConversion, blueBlobColorThresholds, colorBlobFilterContourParams,
                RobotParams.cameraRect, RobotParams.worldRect, true, tracer);
            blueBlobProcessor = blueBlobVision.getVisionProcessor();
            visionProcessorList.add(blueBlobProcessor);
        }

        if (RobotParams.Preferences.useTensorFlowVision)
        {
            robot.globalTracer.traceInfo(moduleName, "Starting TensorFlowVision...");
            tensorFlowVision = new FtcVisionTensorFlow(
                null, TFOD_MODEL_ASSET, TFOD_TARGET_LABELS, RobotParams.cameraRect, RobotParams.worldRect, tracer);
            tensorFlowProcessor = tensorFlowVision.getVisionProcessor();
            tensorFlowProcessor.setMinResultConfidence(TFOD_MIN_CONFIDENCE);
            visionProcessorList.add(tensorFlowProcessor);
        }

        VisionProcessor[] visionProcessors = new VisionProcessor[visionProcessorList.size()];
        visionProcessorList.toArray(visionProcessors);
        vision = RobotParams.Preferences.useWebCam ?
            new FtcVision(
                opMode.hardwareMap.get(WebcamName.class, RobotParams.HWNAME_WEBCAM),
                RobotParams.CAM_IMAGE_WIDTH, RobotParams.CAM_IMAGE_HEIGHT,
                RobotParams.Preferences.showVisionView, visionProcessors) :
            new FtcVision(
                RobotParams.Preferences.useBuiltinCamBack ?
                    BuiltinCameraDirection.BACK : BuiltinCameraDirection.FRONT,
                RobotParams.CAM_IMAGE_WIDTH, RobotParams.CAM_IMAGE_HEIGHT,
                RobotParams.Preferences.showVisionView, visionProcessors);
        // Disable all vision processors until they are needed.
        setAprilTagVisionEnabled(false);
        setRedBlobVisionEnabled(false);
        setBlueBlobVisionEnabled(false);
        setTensorFlowVisionEnabled(false);
    }   //Vision

    /**
     * This method enables/disables AprilTag vision.
     *
     * @param enabled specifies true to enable, false to disable.
     */
    public void setAprilTagVisionEnabled(boolean enabled)
    {
        if (aprilTagProcessor != null)
        {
            vision.setProcessorEnabled(aprilTagProcessor, enabled);
        }
    }   //setAprilTagVisionEnabled

    /**
     * This method enables/disables RedBlob vision.
     *
     * @param enabled specifies true to enable, false to disable.
     */
    public void setRedBlobVisionEnabled(boolean enabled)
    {
        if (redBlobProcessor != null)
        {
            vision.setProcessorEnabled(redBlobProcessor, enabled);
        }
    }   //setRedBlobVisionEnabled

    /**
     * This method enables/disables BlueBlob vision.
     *
     * @param enabled specifies true to enable, false to disable.
     */
    public void setBlueBlobVisionEnabled(boolean enabled)
    {
        if (blueBlobProcessor != null)
        {
            vision.setProcessorEnabled(blueBlobProcessor, enabled);
        }
    }   //setBlueBlobVisionEnabled

    /**
     * This method enables/disables TensorFlow vision.
     *
     * @param enabled specifies true to enable, false to disable.
     */
    public void setTensorFlowVisionEnabled(boolean enabled)
    {
        if (tensorFlowProcessor != null)
        {
            vision.setProcessorEnabled(tensorFlowProcessor, enabled);
        }
    }   //setTensorFlowVisionEnabled

    /**
     * This method checks if AprilTag vision is enabled.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean isAprilTagVisionEnabled()
    {
        return aprilTagProcessor != null && vision.isVisionProcessorEnabled(aprilTagProcessor);
    }   //isAprilTagVisionEnabled

    /**
     * This method checks if RedBlob vision is enabled.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean isRedBlobVisionEnabled()
    {
        return redBlobProcessor != null && vision.isVisionProcessorEnabled(redBlobProcessor);
    }   //isRedBlobVisionEnabled

    /**
     * This method checks if BlueBlob vision is enabled.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean isBlueBlobVisionEnabled()
    {
        return blueBlobProcessor != null && vision.isVisionProcessorEnabled(blueBlobProcessor);
    }   //isBlueBlobVisionEnabled

    /**
     * This method checks if TensorFlow vision is enabled.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean isTensorFlowVisionEnabled()
    {
        return tensorFlowProcessor != null && vision.isVisionProcessorEnabled(tensorFlowProcessor);
    }   //isTensorFlowVisionEnabled

    /**
     * This method is called by the Arrays.sort to sort the target object by decreasing confidence.
     *
     * @param a specifies the first target
     * @param b specifies the second target.
     * @return negative value if a has higher confidence than b, 0 if a and b have equal confidence, positive value
     *         if a has lower confidence than b.
     */
    private int compareConfidence(
        TrcVisionTargetInfo<FtcVisionTensorFlow.DetectedObject> a,
        TrcVisionTargetInfo<FtcVisionTensorFlow.DetectedObject> b)
    {
        return (int)((b.detectedObj.confidence - a.detectedObj.confidence)*100);
    }   //compareConfidence

}   //class Vision