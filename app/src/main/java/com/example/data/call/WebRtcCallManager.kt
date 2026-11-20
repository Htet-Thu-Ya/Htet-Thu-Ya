package com.example.data.call

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcCallManager(private val context: Context) {

    interface Listener {
        fun onLocalIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String)
        fun onPeerConnected()
        fun onRemoteVideoAvailable(available: Boolean)
        fun onPeerConnectionFailed(reason: String)
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val lock = Any()

    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var listener: Listener? = null

    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()
    private var isFrontCamera = true
    private var isVideoCall = false

    private var savedAudioMode: Int = AudioManager.MODE_NORMAL
    private var savedSpeakerphoneOn: Boolean = false
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    fun startSession(
        isVideo: Boolean,
        listener: Listener,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        release()
        synchronized(lock) {
            this.isVideoCall = isVideo
            this.listener = listener
            this.isFrontCamera = true
            this.remoteDescriptionSet = false
            this.pendingRemoteCandidates.clear()
        }

        try {
            ensureFactoryInitialized()
            val egl = EglBase.create()
            val adm = JavaAudioDeviceModule.builder(appContext)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()

            val pcFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
                .createPeerConnectionFactory()

            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            val pc = pcFactory.createPeerConnection(rtcConfig, createPeerObserver())
                ?: throw IllegalStateException("WebRTC PeerConnection ဖန်တီး၍ မရပါ")

            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            }
            val aSource = pcFactory.createAudioSource(audioConstraints)
            val aTrack = pcFactory.createAudioTrack("ARDAMSa0", aSource).apply {
                setEnabled(true)
            }
            pc.addTrack(aTrack, listOf("ARDAMS"))

            var vSource: VideoSource? = null
            var vTrack: VideoTrack? = null
            var capturer: VideoCapturer? = null
            var texHelper: SurfaceTextureHelper? = null

            if (isVideo) {
                capturer = createCameraCapturer(preferFront = true)
                if (capturer != null) {
                    texHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", egl.eglBaseContext)
                    vSource = pcFactory.createVideoSource(capturer.isScreencast)
                    capturer.initialize(texHelper, appContext, vSource.capturerObserver)
                    runCatching {
                        capturer.startCapture(640, 480, 24)
                    }.onFailure { err ->
                        Log.w(TAG, "Failed to start camera capture at 640x480", err)
                    }
                    vTrack = pcFactory.createVideoTrack("ARDAMSv0", vSource).apply {
                        setEnabled(true)
                    }
                    pc.addTrack(vTrack, listOf("ARDAMS"))
                }
            }

            synchronized(lock) {
                eglBase = egl
                audioDeviceModule = adm
                factory = pcFactory
                peerConnection = pc
                audioSource = aSource
                localAudioTrack = aTrack
                videoSource = vSource
                localVideoTrack = vTrack
                videoCapturer = capturer
                surfaceTextureHelper = texHelper
            }

            configureAudioSession(speakerOn = isVideo)
            mainHandler.post {
                val lr = localRenderer
                if (lr != null && vTrack != null) {
                    initAndAttachRenderer(lr, vTrack, isMirror = isFrontCamera, isOverlay = true)
                }
            }
            onReady()
        } catch (t: Throwable) {
            Log.e(TAG, "WebRTC startSession failed", t)
            release()
            onError(t.localizedMessage ?: "Call စနစ် ဖွင့်၍ မရပါ")
        }
    }

    fun createOffer(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val pc = synchronized(lock) { peerConnection }
        if (pc == null) {
            onError("PeerConnection မရှိပါ")
            return
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(
                MediaConstraints.KeyValuePair(
                    "OfferToReceiveVideo",
                    if (isVideoCall) "true" else "false"
                )
            )
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription) {
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        onSuccess(desc.description)
                    }

                    override fun onSetFailure(error: String?) {
                        onError(error ?: "Local SDP Offer သတ်မှတ်မရပါ")
                    }
                }, desc)
            }

            override fun onCreateFailure(error: String?) {
                onError(error ?: "Call Offer ဖန်တီးမရပါ")
            }
        }, constraints)
    }

    fun acceptOfferAndCreateAnswer(
        offerSdp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val pc = synchronized(lock) { peerConnection }
        if (pc == null) {
            onError("PeerConnection မရှိပါ")
            return
        }
        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                flushPendingRemoteCandidates()
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(
                        MediaConstraints.KeyValuePair(
                            "OfferToReceiveVideo",
                            if (isVideoCall) "true" else "false"
                        )
                    )
                }
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(answerDesc: SessionDescription) {
                        pc.setLocalDescription(object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                onSuccess(answerDesc.description)
                            }

                            override fun onSetFailure(error: String?) {
                                onError(error ?: "Local SDP Answer သတ်မှတ်မရပါ")
                            }
                        }, answerDesc)
                    }

                    override fun onCreateFailure(error: String?) {
                        onError(error ?: "Call Answer ဖန်တီးမရပါ")
                    }
                }, constraints)
            }

            override fun onSetFailure(error: String?) {
                onError(error ?: "Remote Offer ဖတ်ရှု၍ မရပါ")
            }
        }, offerDesc)
    }

    fun setRemoteAnswer(
        answerSdp: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val pc = synchronized(lock) { peerConnection } ?: return
        val shouldSet = synchronized(lock) { !remoteDescriptionSet }
        if (!shouldSet) return

        val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                flushPendingRemoteCandidates()
                onSuccess()
            }

            override fun onSetFailure(error: String?) {
                onError(error ?: "Remote Answer ချိတ်ဆက်မရပါ")
            }
        }, answerDesc)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidateStr: String) {
        val ice = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
        synchronized(lock) {
            val pc = peerConnection
            if (pc != null && remoteDescriptionSet) {
                pc.addIceCandidate(ice)
            } else {
                pendingRemoteCandidates.add(ice)
            }
        }
    }

    fun setMuted(muted: Boolean) {
        synchronized(lock) {
            localAudioTrack?.setEnabled(!muted)
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        synchronized(lock) {
            localVideoTrack?.setEnabled(enabled)
        }
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        applySpeakerphone(enabled)
    }

    fun switchCamera(onDone: (isFront: Boolean) -> Unit = {}) {
        val cam = synchronized(lock) { videoCapturer as? CameraVideoCapturer } ?: return
        cam.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                synchronized(lock) {
                    isFrontCamera = isFront
                }
                mainHandler.post {
                    localRenderer?.setMirror(isFront)
                    onDone(isFront)
                }
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.w(TAG, "switchCamera error: $errorDescription")
            }
        })
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        mainHandler.post {
            synchronized(lock) {
                localRenderer = renderer
            }
            val track = synchronized(lock) { localVideoTrack }
            initAndAttachRenderer(renderer, track, isMirror = isFrontCamera, isOverlay = true)
        }
    }

    fun detachLocalRenderer(renderer: SurfaceViewRenderer) {
        mainHandler.post {
            val track = synchronized(lock) {
                if (localRenderer == renderer) localRenderer = null
                localVideoTrack
            }
            runCatching { track?.removeSink(renderer) }
            runCatching { renderer.release() }
        }
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        mainHandler.post {
            synchronized(lock) {
                remoteRenderer = renderer
            }
            val track = synchronized(lock) { remoteVideoTrack }
            initAndAttachRenderer(renderer, track, isMirror = false, isOverlay = false)
        }
    }

    fun detachRemoteRenderer(renderer: SurfaceViewRenderer) {
        mainHandler.post {
            val track = synchronized(lock) {
                if (remoteRenderer == renderer) remoteRenderer = null
                remoteVideoTrack
            }
            runCatching { track?.removeSink(renderer) }
            runCatching { renderer.release() }
        }
    }

    private fun initAndAttachRenderer(
        renderer: SurfaceViewRenderer,
        track: VideoTrack?,
        isMirror: Boolean,
        isOverlay: Boolean
    ) {
        val egl = synchronized(lock) { eglBase } ?: return
        runCatching {
            renderer.init(egl.eglBaseContext, null)
        }
        runCatching {
            renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            renderer.setEnableHardwareScaler(true)
            renderer.setMirror(isMirror)
            renderer.setZOrderMediaOverlay(isOverlay)
            track?.addSink(renderer)
        }
    }

    fun release() {
        val lr: SurfaceViewRenderer?
        val rr: SurfaceViewRenderer?
        val lvt: VideoTrack?
        val rvt: VideoTrack?
        val cap: VideoCapturer?
        val pc: PeerConnection?
        val aSrc: AudioSource?
        val vSrc: VideoSource?
        val tex: SurfaceTextureHelper?
        val adm: JavaAudioDeviceModule?
        val fac: PeerConnectionFactory?
        val egl: EglBase?

        synchronized(lock) {
            listener = null
            remoteDescriptionSet = false
            pendingRemoteCandidates.clear()
            lr = localRenderer
            rr = remoteRenderer
            localRenderer = null
            remoteRenderer = null
            lvt = localVideoTrack
            rvt = remoteVideoTrack
            localVideoTrack = null
            remoteVideoTrack = null
            localAudioTrack = null
            cap = videoCapturer
            videoCapturer = null
            pc = peerConnection
            peerConnection = null
            aSrc = audioSource
            audioSource = null
            vSrc = videoSource
            videoSource = null
            tex = surfaceTextureHelper
            surfaceTextureHelper = null
            adm = audioDeviceModule
            audioDeviceModule = null
            fac = factory
            factory = null
            egl = eglBase
            eglBase = null
        }

        mainHandler.post {
            if (lr != null) {
                runCatching { lvt?.removeSink(lr) }
                runCatching { lr.release() }
            }
            if (rr != null) {
                runCatching { rvt?.removeSink(rr) }
                runCatching { rr.release() }
            }
        }

        runCatching { cap?.stopCapture() }
        runCatching { cap?.dispose() }
        runCatching { tex?.dispose() }
        runCatching { aSrc?.dispose() }
        runCatching { vSrc?.dispose() }
        runCatching { pc?.close() }
        runCatching { pc?.dispose() }
        runCatching { adm?.release() }
        runCatching { fac?.dispose() }
        runCatching { egl?.release() }
        restoreAudioSession()
    }

    private fun flushPendingRemoteCandidates() {
        synchronized(lock) {
            remoteDescriptionSet = true
            val pc = peerConnection ?: return
            pendingRemoteCandidates.forEach { pc.addIceCandidate(it) }
            pendingRemoteCandidates.clear()
        }
    }

    private fun createPeerObserver(): PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate ?: return
            val cb = synchronized(lock) { listener } ?: return
            cb.onLocalIceCandidate(
                sdpMid = candidate.sdpMid ?: "0",
                sdpMLineIndex = candidate.sdpMLineIndex.coerceIn(0, 32),
                candidate = candidate.sdp
            )
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            when (newState) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    synchronized(lock) { listener }?.onPeerConnected()
                }
                PeerConnection.PeerConnectionState.FAILED -> {
                    synchronized(lock) { listener }?.onPeerConnectionFailed("Network connection failed")
                }
                else -> Unit
            }
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            if (newState == PeerConnection.IceConnectionState.CONNECTED ||
                newState == PeerConnection.IceConnectionState.COMPLETED
            ) {
                synchronized(lock) { listener }?.onPeerConnected()
            }
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            val track = transceiver?.receiver?.track()
            if (track is VideoTrack) {
                synchronized(lock) {
                    remoteVideoTrack = track
                }
                mainHandler.post {
                    val rr = synchronized(lock) { remoteRenderer }
                    if (rr != null) {
                        initAndAttachRenderer(rr, track, isMirror = false, isOverlay = false)
                    }
                    synchronized(lock) { listener }?.onRemoteVideoAvailable(true)
                }
            }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: MediaStream?) {
            val vTrack = stream?.videoTracks?.firstOrNull() ?: return
            synchronized(lock) {
                remoteVideoTrack = vTrack
            }
            mainHandler.post {
                val rr = synchronized(lock) { remoteRenderer }
                if (rr != null) {
                    initAndAttachRenderer(rr, vTrack, isMirror = false, isOverlay = false)
                }
                synchronized(lock) { listener }?.onRemoteVideoAvailable(true)
            }
        }
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(dataChannel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) = Unit
    }

    private fun createCameraCapturer(preferFront: Boolean): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(appContext)) {
            Camera2Enumerator(appContext)
        } else {
            Camera1Enumerator(false)
        }
        val names = enumerator.deviceNames
        val preferred = names.firstOrNull { name ->
            if (preferFront) enumerator.isFrontFacing(name) else enumerator.isBackFacing(name)
        } ?: names.firstOrNull() ?: return null
        return enumerator.createCapturer(preferred, null)
    }

    private fun configureAudioSession(speakerOn: Boolean) {
        savedAudioMode = audioManager.mode
        @Suppress("DEPRECATION")
        savedSpeakerphoneOn = audioManager.isSpeakerphoneOn

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest = req
            hasAudioFocus = audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            hasAudioFocus = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        applySpeakerphone(speakerOn)
    }

    private fun applySpeakerphone(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetType = if (enabled) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == targetType }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
        }
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = enabled
    }

    private fun restoreAudioSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.clearCommunicationDevice() }
        } else {
            @Suppress("DEPRECATION")
            runCatching { audioManager.isSpeakerphoneOn = savedSpeakerphoneOn }
        }
        runCatching { audioManager.mode = savedAudioMode }
        if (hasAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
            } else {
                @Suppress("DEPRECATION")
                runCatching { audioManager.abandonAudioFocus(null) }
            }
            hasAudioFocus = false
        }
    }

    private fun ensureFactoryInitialized() {
        if (factoryInitialized) return
        synchronized(WebRtcCallManager::class.java) {
            if (!factoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                factoryInitialized = true
            }
        }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    companion object {
        private const val TAG = "WebRtcCallManager"

        @Volatile
        private var factoryInitialized = false
    }
}
