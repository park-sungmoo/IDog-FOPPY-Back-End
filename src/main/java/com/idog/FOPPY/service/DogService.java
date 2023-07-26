package com.idog.FOPPY.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idog.FOPPY.domain.Breed;
import com.idog.FOPPY.domain.Dog;
import com.idog.FOPPY.domain.User;
import com.idog.FOPPY.dto.dog.DogResponse;
import com.idog.FOPPY.dto.dog.MissingDogResponse;
import com.idog.FOPPY.dto.dog.MissingInfoRequest;
import com.idog.FOPPY.dto.dog.DogInfoRequest;
import com.idog.FOPPY.repository.DogRepository;
import com.idog.FOPPY.repository.DogSpecification;
import com.idog.FOPPY.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class DogService {

    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final S3Service s3Service;

    @Transactional
    public Long save(DogInfoRequest dogInfoRequest, List<MultipartFile> multipartFile) throws IOException, InterruptedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (String) authentication.getPrincipal();

        if (multipartFile.size() > 10) {
            throw new IllegalArgumentException("최대 10장까지 가능합니다.");
        }

        User user = userRepository.findByEmail(userEmail)  // Assuming you have a findByUsername method
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userEmail));

        List<String> imgUrlList = s3Service.upload(multipartFile, "/dog");

        // Create OkHttpClient
        OkHttpClient client = new OkHttpClient();

// Create multipart body builder
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

// Add files to the multipart request
        for (MultipartFile file : multipartFile) {
            multipartBodyBuilder.addFormDataPart("files", file.getOriginalFilename(),
                    RequestBody.create(file.getBytes(), MediaType.parse(file.getContentType())));
        }

// Build the Request
        Request request = new Request.Builder()
                .url("http://18.208.109.190:8000/noseDetect") // Replace with your FastAPI URL
                .post(multipartBodyBuilder.build())
                .build();

        // Send the POST request and get the response
        Response response = client.newCall(request).execute();

        // Parse the response JSON to get the list of URLs
        JsonNode responseJson = new ObjectMapper().readTree(response.body().string());
        List<String> noseImgUrlList = new ArrayList<>();
        if (responseJson.has("result")) {
            for (JsonNode urlList : responseJson.get("result")) {
                for (JsonNode urlNode : urlList) {
                    noseImgUrlList.add(urlNode.asText());
                }
            }
        }
        Dog dog = user.addDog(dogInfoRequest.getName(), dogInfoRequest.getBirth(), dogInfoRequest.getSex(), dogInfoRequest.getBreed(),
                dogInfoRequest.getNote(), dogInfoRequest.getDisease(), dogInfoRequest.getNeutered(), imgUrlList, noseImgUrlList);

        dogRepository.save(dog);

        return dog.getId();

    }
        public Long setMissing(Long id, MissingInfoRequest missingInfo) {
        Dog dog = dogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid dog ID"));

        dog.markAsMissing(missingInfo);

        dogRepository.save(dog);

        return dog.getId();
    }

    @Transactional
    public List<MissingDogResponse> getMissingDogs(String missingGu, String missingDong, LocalDate startDate, LocalDate endDate, Breed breed) {
        List<Dog> dogs = dogRepository.findAll(DogSpecification.missingAndAreaAndDateAndBreed(
                missingGu, missingDong, startDate, endDate, breed));

        return dogs.stream()
                .map(MissingDogResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<DogResponse> getMyDogs() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (String)authentication.getPrincipal();

        User user = userRepository.findByEmail(userEmail)  // Assuming you have a findByUsername method
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userEmail));

        List<Dog> dogs = dogRepository.findByUser(user);

        List<DogResponse> dogResponses = dogs.stream()
                .map(DogResponse::new)
                .collect(Collectors.toList());

        return dogResponses;
    }
}
