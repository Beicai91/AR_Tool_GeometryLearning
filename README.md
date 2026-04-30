# AR_Tool_GeometryLearning

This is a prototype application developed as part of a master’s thesis (**MALTT**: Master of Learning and Teaching Technologies) that explores the use of marker-based augmented reality for interactive geometry construction.
The system maps physically manipulated markers to abstract geometric point and updates geometric constructions (segment, 2D figures) **in real time based on user actions**. The project focuses on **robust marker detection**, **real-time interaction** and **geometric computation** within an educational context. 

## Machine
This application is developed using **HP Sprout** which integrates a camera for detection, a projector for display and a touch mat. However, in principle, it could also run on other machines offering equivalent camera-projector functionality. 
<img width="944" height="908" alt="图片" src="https://github.com/user-attachments/assets/7404a1cc-c4f6-4d49-a280-8d18bf9172a3" />


## Dev features:
- Marker-based detection and tracking: Reliable detection of QR-assisted tangible markers under variable lighting conditions
- Tangible input mapped to geometric point entities: physical markers are mapped to point entities used in geometric computation
- Real-time geometric construction following marker movement: geometric segments and figures are dynamically updated as markers are manipulated
- Real-time interaction feedback: geometric actions are validated at runtime, producing distinct visual outcomes for valid and invalid segment prolongation
- Conditional visualisation of invisible geometric entities: projection of underlying geometric line for segment prolongation triggered by hint button
- Phase-based interaction flow: exploration mode followed by task-oriented mode

## Technical notes:
Robust marker detection was a central challenge due to the limitation of HP Sprout’s hardware, environmental noises (light, hand movements etc.) and projected graphics. 

- **Detection approach**
The initial shape/color-based marker detection proved unstable in uncontrolled environments. To improve robustness, the system was redesigned to use QR-assisted markers, providing reliable identification and persistent marker IDs across frames. 

- **Projection offset handling**
Due to pedagogical design, each QR code is paired with a circular marker that serves as the geometric anchor. The QR code provides detection, identification, and orientation, while coordinates are remapped to the circular marker using a **rotation-aware offset**, ensuring that projected geometry connects only between anchor markers and does not overlap the QR codes.
<img width="351" height="218" alt="Capture d’écran 2026-04-01 à 23 15 03" src="https://github.com/user-attachments/assets/44313b9d-ba3d-4582-beb0-24ceffcb3a09" />

- **Temporal filtering of marker states**
The system maintains two separate data structures:
1. a **raw marker** set, updated every frame from the detection layer
2. a **tracked marker** set, used by all geometric computations

Tracked markers are only removed after being absent for several consecutive frames. This temporal filtering reduces visual flickering and prevents transient detection noise from affecting downstream geometric operations. 
This separation allows the system to remain responsive while maintaining stable interaction behaviour under noisy input conditions.

## Demo
The following demos show two basic functionalities of the system: marker detection and real-time tracking and projection. 

![Demo-ezgif com-resize(1)](https://github.com/user-attachments/assets/245d9a3f-c596-4147-a0dd-7cdb809cc749)

![Demo-ezgif com-resize(2)](https://github.com/user-attachments/assets/c835e898-5a19-430c-a645-10122a3c645b)

